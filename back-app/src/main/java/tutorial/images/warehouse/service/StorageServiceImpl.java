package tutorial.images.warehouse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import tutorial.images.warehouse.config.AwsProperties;
import tutorial.images.warehouse.dto.ImageContent;
import tutorial.images.warehouse.dto.ImageInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {
    private final S3AsyncClient s3AsyncClient;
    private final AwsProperties awsProperties;

    @Override
    public CompletableFuture<Optional<List<ImageInfo>>> imageList() {
        return getList()
                .thenApply(response -> Optional.of(response.contents().stream()
                        .map(S3Object::key)
                        .filter(key -> key.startsWith("full-"))
                        .map(key -> key.substring(5))
                        .map(ImageInfo::new)
                        .toList())
                )
                .exceptionally(ex -> handleException("list error", ex));
    }

    @Override
    public CompletableFuture<Optional<ImageContent>> imageContentThumbnail(String id) {
        return objectExists("thumbnail-" + id)
                .thenCompose(exists -> downloadContent(exists ? "thumbnail-" + id : "full-" + id))
                .thenApply(response -> Optional.of(createImageContent(response)))
                .exceptionally(ex -> handleException("get content error", ex));
    }

    @Override
    public CompletableFuture<Optional<ImageContent>> imageContentOriginal(String id) {
        return downloadContent("full-" + id)
                .thenApply(response -> Optional.of(createImageContent(response)))
                .exceptionally(ex -> handleException("get content error", ex));
    }

    @Override
    public CompletableFuture<Optional<String>> create(String name, String contentType, byte[] content) {
        final String key = UUID.randomUUID().toString();
        return uploadContent(key, name, contentType, content)
                .thenApply(resp -> Optional.of(key))
                .exceptionally(ex -> handleException("create error", ex));
    }

    private <T> Optional<T> handleException(String msg, Throwable ex) {
        log.error(msg, ex);
        return Optional.empty();
    }

    private static ImageContent createImageContent(ResponseBytes<GetObjectResponse> response) {
        return new ImageContent(response.response().contentType(), response.asByteArray());
    }

    private CompletableFuture<ListObjectsResponse> getList() {
        final ListObjectsRequest request = ListObjectsRequest
                .builder()
                .bucket(awsProperties.s3bucket())
                .build();
        return s3AsyncClient.listObjects(request);
    }

    private CompletableFuture<PutObjectResponse> uploadContent(
            String id,
            String name,
            String contentType,
            byte[] content
    ) {
        final PutObjectRequest request = PutObjectRequest
                .builder()
                .bucket(awsProperties.s3bucket())
                .key("full-" + id)
                .contentType(contentType)
                .contentDisposition("attachment; filename=\"" + name + "\"")
                .build();
        final AsyncRequestBody requestBody = AsyncRequestBody.fromBytes(content);
        return s3AsyncClient.putObject(request, requestBody);
    }

    private CompletableFuture<ResponseBytes<GetObjectResponse>> downloadContent(String id) {
        final GetObjectRequest getObjectRequest = GetObjectRequest
                .builder()
                .bucket(awsProperties.s3bucket())
                .key(id)
                .build();
        return s3AsyncClient.getObject(getObjectRequest, AsyncResponseTransformer.toBytes());
    }

    private CompletableFuture<Boolean> objectExists(String key) {
        final HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(awsProperties.s3bucket())
                .key(key)
                .build();
        return s3AsyncClient.headObject(request)
                .handle((response, ex) -> {
                    if (ex == null) {
                        return true;
                    }
                    final Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                    if (cause instanceof S3Exception e && e.statusCode() == 404) {
                        return false;
                    }
                    throw new CompletionException(cause);
                });
    }

}
