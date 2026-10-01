#!/usr/bin/env bash

set -e

LAMBDA_NAME="thumbnail-lambda"
ZIP_FILE="../lambda-handlers/build/distributions/thumbnail-lambda.zip"

ENDPOINT="http://localhost:4567"

if ! test -f "$ZIP_FILE"; then
    echo "Lambda ZIP not found: $ZIP_FILE"
    echo "Run: ./gradlew lambdaZip"
    exit 1
fi

if aws --endpoint-url="$ENDPOINT" lambda get-function \
        --function-name "$LAMBDA_NAME" >/dev/null 2>&1; then

    echo "Updating $LAMBDA_NAME..."

    aws --endpoint-url="$ENDPOINT" lambda update-function-code \
        --function-name "$LAMBDA_NAME" \
        --zip-file "fileb://$ZIP_FILE"

else

    echo "Creating $LAMBDA_NAME..."

    aws --endpoint-url="$ENDPOINT" lambda create-function \
        --function-name "$LAMBDA_NAME" \
        --runtime java21 \
        --handler tutorial.images.warehouse.ThumbnailHandler::handleRequest \
        --role arn:aws:iam::000000000000:role/lambda-role \
        --zip-file "fileb://$ZIP_FILE"
fi

echo "Waiting for Lambda to become active..."

aws \
    --endpoint-url="$ENDPOINT" \
    lambda wait function-active-v2 \
    --function-name "$LAMBDA_NAME"

echo "Lambda is active."