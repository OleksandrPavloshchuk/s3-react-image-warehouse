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
                        .filter(key -> key.startsWith("thumbnail-"))
                        .map(ImageInfo::new)
                        .toList())
                )
                .exceptionally(ex -> {
                    log.error("list error", ex);
                    return Optional.empty();
                });
    }

    @Override
    public CompletableFuture<Optional<ImageContent>> imageContent(String id) {
        return downloadContent(id)
                .thenApply(response ->
                        Optional.of(new ImageContent(response.response().contentType(), response.asByteArray())))
                .exceptionally(ex -> {
                    log.error("get content error", ex);
                    return Optional.empty();
                });
    }

    @Override
    public CompletableFuture<Optional<String>> create(String name, String contentType, byte[] content) {
        final String key = UUID.randomUUID().toString();
        return uploadContent(key, name, contentType, content)
                .thenApply(resp -> Optional.of(key))
                .exceptionally(ex -> {
                    log.error("create error", ex);
                    return Optional.empty();
                });
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


}
