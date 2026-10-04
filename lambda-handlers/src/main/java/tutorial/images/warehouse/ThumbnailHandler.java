package tutorial.images.warehouse;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ThumbnailHandler {

    private static final String BUCKET = "image-warehouse";

    private final S3Client s3 = createS3Client();

    public void handleRequest(S3Event event, Context context) throws IOException {
        final ThumbnailImageCreator thumbnailImageCreator = new ThumbnailImageCreator(200);
        for (S3EventNotification.S3EventNotificationRecord record : event.getRecords()) {
            final String bucket = record.getS3().getBucket().getName();
            final String key = record.getS3().getObject().getKey();
            if (bucket.equals(BUCKET) && key.startsWith("full-")) {
                final ResponseBytes<GetObjectResponse> sourceImageResponse = openSourceImage(key);
                try (final InputStream srcImageStream = sourceImageResponse.asInputStream()) {
                    final ByteArrayOutputStream destImage = thumbnailImageCreator.create(srcImageStream);
                    saveThumbnail(createThumbnailKey(key), destImage);
                }
            }
        }
    }

    private static String createThumbnailKey(String srcKey) {
        return "thumbnail-" + srcKey.substring(5);
    }

    private void saveThumbnail(String thumbnailKey, ByteArrayOutputStream destImage) {
        final PutObjectRequest request = PutObjectRequest
                .builder()
                .bucket(BUCKET)
                .key(thumbnailKey)
                .contentType("image/jpeg")
                .build();
        s3.putObject(request, RequestBody.fromBytes(destImage.toByteArray()));
    }

    private ResponseBytes<GetObjectResponse> openSourceImage(String key) {
        final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .build();
        return s3.getObject(getObjectRequest, ResponseTransformer.toBytes());
    }

    private static S3Client createS3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create("http://localstack-image-warehouse:4566"))
                .region(Region.EU_NORTH_1)
                .forcePathStyle(true)
                .build();
    }
}
