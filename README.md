# IMAGE WAREHOUSE

## Purpose

This application is developed to gain some experience with S3 storage for binary files, using of `useRef()` React hook
and files uploading using unmanaged forms.

## Application parts

* `docker/docker-compose` - used to run AWS LocalStack for development
* `back-app` - backend part, written on Spring Boot using Gradle
* `front-app` - frontend part, written on React using Vite
* `lambda-handlers` - executed inside AWS for generation of the thumbnail from source image

## Init LocalStack

**Start LocalStack:**

```shell
./docker/docker compose up
```

LocalStack uses:

```yaml
services:
  localstack-image-warehouse:
    image: localstack/localstack:3.8.1
    ports:
      - "4567:4566"
    environment:
      - SERVICES=s3,lambda
    volumes:
      - "/var/run/docker.sock:/var/run/docker.sock"
```

The Docker socket is required because LocalStack runs Lambda functions using Docker.

The port mapping is important:

```text
host:      localhost:4567
container: localstack-image-warehouse:4566
```

AWS CLI commands executed on the host use:

```text
http://localhost:4567
```

The Lambda itself runs in another Docker container. Therefore, `localhost:4567` cannot be used from Lambda to access LocalStack.

Lambda uses:

```text
http://localstack-image-warehouse:4566
```

because `localstack-image-warehouse` is the LocalStack service name in the Docker network.

---

**Create bucket:**

```shell
./script/create-bucket.sh
```

---

## Build and pack Lambda

From the project root:

```shell
./gradlew build
./gradlew lambdaZip
```

The following file is created after successful execution:

```text
./lambda-handlers/build/distributions/thumbnail-lambda.zip
```

The ZIP must contain the Lambda handler class and all runtime dependencies.

The Gradle task uses `zipTree()` for the application JAR. The JAR must not be stored as a nested JAR inside the Lambda ZIP:

```groovy
tasks.register('lambdaZip', Zip) {
    dependsOn tasks.jar
    archiveFileName = 'thumbnail-lambda.zip'
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from {
        configurations.runtimeClasspath.collect { zipTree(it) }
    }

    from {
        zipTree(tasks.jar.archiveFile)
    }
}
```

The handler is:

```text
tutorial.images.warehouse.ThumbnailHandler::handleRequest
```

The handler method must use the AWS Lambda `Context`:

```java
import com.amazonaws.services.lambda.runtime.Context;
```

and not an unrelated class such as:

```java
import javax.naming.Context;
```

---

## Deploy Lambda

```shell
./script/deploy-lambda.sh
```

The deployment script creates the Lambda if it does not exist, otherwise updates its code.

After deployment the script waits until Lambda becomes active.

The Lambda uses:

```text
runtime: java21
name: thumbnail-lambda
region: eu-north-1
```

The Lambda execution role is only a LocalStack placeholder:

```text
arn:aws:iam::000000000000:role/lambda-role
```

---

## Configure S3 → Lambda notification

Creating the Lambda is not enough.

S3 must be explicitly configured to invoke the Lambda when a source image is created.

Create:

```text
docker/notification.json
```

with:

```json
{
  "LambdaFunctionConfigurations": [
    {
      "LambdaFunctionArn": "arn:aws:lambda:eu-north-1:000000000000:function:thumbnail-lambda",
      "Events": [
        "s3:ObjectCreated:*"
      ],
      "Filter": {
        "Key": {
          "FilterRules": [
            {
              "Name": "prefix",
              "Value": "full-"
            }
          ]
        }
      }
    }
  ]
}
```

Apply the configuration:

```shell
aws --endpoint-url=http://localhost:4567 \
    s3api put-bucket-notification-configuration \
    --bucket image-warehouse \
    --notification-configuration file://docker/notification.json
```

Check it:

```shell
aws --endpoint-url=http://localhost:4567 \
    s3api get-bucket-notification-configuration \
    --bucket image-warehouse
```

The configuration must contain:

```text
Events:      s3:ObjectCreated:*
Prefix:      full-
Lambda ARN:  arn:aws:lambda:eu-north-1:000000000000:function:thumbnail-lambda
```

The `full-` prefix is important.

The Lambda creates:

```text
thumbnail-<uuid>
```

and this object must not trigger the same Lambda again.

Therefore:

```text
full-*       → Lambda
thumbnail-*  → no Lambda invocation
```

---

## Allow S3 to invoke Lambda

S3 notification configuration alone is not sufficient.

Lambda also needs a resource-based policy allowing the S3 service to invoke it.

Check the policy:

```shell
aws --endpoint-url=http://localhost:4567 \
    lambda get-policy \
    --function-name thumbnail-lambda
```

If the policy does not exist, add it:

```shell
aws --endpoint-url=http://localhost:4567 \
    lambda add-permission \
    --function-name thumbnail-lambda \
    --statement-id s3-invoke \
    --action lambda:InvokeFunction \
    --principal s3.amazonaws.com \
    --source-arn arn:aws:s3:::image-warehouse
```

The important part of the resulting policy is:

```text
Principal: s3.amazonaws.com
Action:    lambda:InvokeFunction
SourceArn: arn:aws:s3:::image-warehouse
```

This means that S3 is allowed to invoke this Lambda, and the permission is restricted to the `image-warehouse` bucket.

---

## Verify Lambda manually

Before testing the complete S3 → Lambda pipeline, Lambda can be invoked directly:

```shell
aws \
    --endpoint-url=http://localhost:4567 \
    lambda invoke \
    --function-name thumbnail-lambda \
    --cli-binary-format raw-in-base64-out \
    --payload '{"test":"hello"}' \
    /tmp/lambda-response.json

cat /tmp/lambda-response.json
```

This verifies that:

* Lambda exists;
* the ZIP is valid;
* the Java runtime starts;
* the handler can be found and executed.

The payload above is not a real S3 event, so this test does not verify S3 integration.

---

## Test complete pipeline

Upload an image through the application.

The expected sequence is:

```text
Spring Boot
    |
    | PutObject
    v
S3
    |
    | ObjectCreated
    | prefix = full-
    v
Lambda
    |
    | GetObject
    | ImageIO.read()
    | resize
    | ImageIO.write()
    | PutObject
    v
S3
```

Check the bucket:

```shell
aws --endpoint-url=http://localhost:4567 \
    s3 ls s3://image-warehouse
```

Expected result:

```text
full-<uuid>
thumbnail-<same-uuid>
```

For example:

```text
247381  full-7d93f285-cc35-4462-9f4f-9061ae4f95d6
 31903  thumbnail-7d93f285-cc35-4462-9f4f-9061ae4f95d6
```

The thumbnail is generated asynchronously. Therefore, it can appear several seconds after the original image.

---

## Troubleshooting

### `full-*` appears, but no `thumbnail-*`

First check the S3 notification:

```shell
aws --endpoint-url=http://localhost:4567 \
    s3api get-bucket-notification-configuration \
    --bucket image-warehouse
```

If it returns an empty configuration, configure the notification as described above.

Then check the Lambda policy:

```shell
aws --endpoint-url=http://localhost:4567 \
    lambda get-policy \
    --function-name thumbnail-lambda
```

If the policy does not exist, add the S3 permission.

---

### LocalStack log does not contain Lambda invocation

After uploading `full-*`, the LocalStack log should contain activity corresponding to the Lambda invocation.

The important sequence is approximately:

```text
AWS s3.PutObject => 200
...
AWS s3.GetObject => 200
...
AWS s3.PutObject => 200
```

and LocalStack's Lambda runtime should show the invocation lifecycle.

If there is no Lambda activity at all, investigate S3 notification and Lambda permission before changing the Java code.

---

### Lambda cannot find the handler

Check the ZIP:

```shell
unzip -l ./lambda-handlers/build/distributions/thumbnail-lambda.zip \
    | grep ThumbnailHandler
```

Make sure the handler is:

```text
tutorial.images.warehouse.ThumbnailHandler::handleRequest
```

and that the ZIP contains the class itself rather than only a nested JAR.

---

### Lambda cannot access LocalStack S3

Do not use:

```text
http://localhost:4567
```

inside Lambda.

Use:

```text
http://localstack-image-warehouse:4566
```

The first address is for clients running on the host. The second is for clients running inside the Docker network.

---

## Architecture

The final architecture intentionally does not invoke Lambda from Spring Boot.

```text
                       ┌─────────────────┐
                       │   Spring Boot   │
                       └────────┬────────┘
                                │
                                │ PutObject
                                ▼
                       ┌─────────────────┐
                       │       S3        │
                       │                 │
                       │   full-<uuid>   │
                       └────────┬────────┘
                                │
                         ObjectCreated
                          prefix = full-
                                │
                                ▼
                       ┌─────────────────┐
                       │     Lambda      │
                       │                 │
                       │    GetObject    │
                       │    resize       │
                       │    PutObject    │
                       └────────┬────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │       S3        │
                       │                 │
                       │ thumbnail-<uuid>│
                       └─────────────────┘
```

This gives the application an asynchronous thumbnail-generation pipeline:

```text
POST/upload
    ↓
full image stored
    ↓
request can finish
    ↓
thumbnail generated asynchronously
```

Spring Boot does not need to know that Lambda exists.

This also means that the S3 event mechanism, rather than application code, is responsible for connecting image storage with thumbnail generation.
