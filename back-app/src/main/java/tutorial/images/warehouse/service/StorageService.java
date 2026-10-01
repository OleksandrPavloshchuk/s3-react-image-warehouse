package tutorial.images.warehouse.service;

import tutorial.images.warehouse.dto.ImageContent;
import tutorial.images.warehouse.dto.ImageInfo;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StorageService {
    CompletableFuture<Optional<List<ImageInfo>>> imageList();
    CompletableFuture<Optional<ImageContent>> imageContent(String id);
    CompletableFuture<Optional<String>> create(String name, String contentType, byte[] bytes);
}
