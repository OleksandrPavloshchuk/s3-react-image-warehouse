package tutorial.images.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tutorial.images.warehouse.dto.ImageContent;
import tutorial.images.warehouse.dto.ImageInfo;
import tutorial.images.warehouse.service.StorageService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@RestController
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @GetMapping
    public CompletableFuture<List<ImageInfo>> imageList() {
        return storageService.imageList()
                .thenApply(ol -> ol.orElseGet(List::of));
    }

    @GetMapping("/{id}/thumbnail")
    public CompletableFuture<ResponseEntity<byte[]>> imageContentThumbnail(@PathVariable String id) {
        return imageContent(id, storageService::imageContentThumbnail);
    }

    @GetMapping("/{id}/original")
    public CompletableFuture<ResponseEntity<byte[]>> imageContentOriginal(@PathVariable String id) {
        return imageContent(id, storageService::imageContentOriginal);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<?>> create(@RequestParam("file") MultipartFile file) throws IOException {
        final String contentType = file.getContentType();
        final String name = file.getName();
        final byte[] bytes = file.getBytes();
        return storageService.create(name, contentType, bytes)
                .thenApply(oc -> oc
                        .map(StorageController::createCreatedResponse)
                        .orElseGet(() -> ResponseEntity.badRequest().build()));
    }

    private CompletableFuture<ResponseEntity<byte[]>> imageContent(
            String id,
            Function<String, CompletableFuture<Optional<ImageContent>>> reader
    ) {
        return reader.apply(id)
                .thenApply(oc -> oc.map(
                                content -> ResponseEntity
                                        .ok()
                                        .contentType(MediaType.parseMediaType(content.type()))
                                        .body(content.data()))
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    private static ResponseEntity<?> createCreatedResponse(String id) {
        try {
            return ResponseEntity.created(new URI("/" + id)).build();
        } catch (URISyntaxException ex) {
            // TODO create better response
            return ResponseEntity.badRequest().build();
        }
    }
}
