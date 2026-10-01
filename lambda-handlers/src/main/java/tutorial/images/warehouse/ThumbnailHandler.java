package tutorial.images.warehouse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ThumbnailHandler {

    public void handleRequest(S3Event event, Context context) throws IOException {
        try (final S3Client s3 = createS3Client()) {
            for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
                final String bucket = record.getS3().getBucket().getName();
                final String key = record.getS3().getObject().getKey();
                if (bucket.equals("image-warehouse") && key.startsWith("full-")) {

                    final String id = key.substring(5);
                    final String thumbnailKey = "thumbnail-" + id;

                    final ResponseBytes<GetObjectResponse> fullImageResponse = openFullImage(s3, bucket, key);
                    try (final InputStream fullImageStream = fullImageResponse.asInputStream()) {

                        final BufferedImage fullImage = ImageIO.read(fullImageStream);

                        context.getLogger().log("Full image: " + fullImage);

                        if (fullImage != null) {
                            final BufferedImage thumbnailImage = createThumbnail(fullImage);
                            final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                            final boolean written = ImageIO.write(thumbnailImage, "jpeg", out);
                            if (!written) {
                                throw new IOException("No JPEG writer available");
                            }

                            final PutObjectRequest request = PutObjectRequest
                                    .builder()
                                    .bucket(bucket)
                                    .key(thumbnailKey)
                                    .contentType("image/jpeg")
                                    .build();
                            final RequestBody requestBody = RequestBody.fromBytes(out.toByteArray());
                            s3.putObject(request, requestBody);
                        }
                    }
                }
            }
        }
    }

    private ResponseBytes<GetObjectResponse> openFullImage(S3Client s3, String bucket, String key) {
        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3.getObject(getObjectRequest, ResponseTransformer.toBytes());
    }

    private static BufferedImage createThumbnail(BufferedImage fullImage) {
        final int maxWidth = 300;

        final int width = fullImage.getWidth();
        final int height = fullImage.getHeight();

        final int newWidth = Math.min(width, maxWidth);
        final int newHeight = (int) Math.round(
                (double) height * newWidth / width
        );

        final BufferedImage thumbnail = new BufferedImage(
                newWidth,
                newHeight,
                BufferedImage.TYPE_INT_RGB
        );

        final Graphics2D graphics = thumbnail.createGraphics();

        try {
            graphics.drawImage(
                    fullImage,
                    0,
                    0,
                    newWidth,
                    newHeight,
                    null
            );
        } finally {
            graphics.dispose();
        }
        return thumbnail;
    }

    private static S3Client createS3Client() {
        return S3Client.builder()
                .endpointOverride(
                        URI.create("http://localstack-image-warehouse:4566")
                )
                .region(Region.EU_NORTH_1)
                .forcePathStyle(true)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        System.getenv("AWS_ACCESS_KEY"),
                                        System.getenv("AWS_SECRET_ACCESS_KEY")
                                )
                        )
                )
                .build();
    }
}
