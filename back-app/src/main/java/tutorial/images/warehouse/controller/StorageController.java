package tutorial.images.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tutorial.images.warehouse.dto.ImageInfo;
import tutorial.images.warehouse.service.StorageService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @GetMapping
    public CompletableFuture<List<ImageInfo>> imageList() {
        return storageService.imageList()
                .thenApply( ol -> ol.orElseGet(List::of));
    }

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<byte[]>> imageContent(@PathVariable String id) {
        return storageService.imageContent(id)
                .thenApply(oc -> oc.map(
                                content -> ResponseEntity
                                        .ok()
                                        .contentType(MediaType.parseMediaType(content.type()))
                                        .body(content.data()))
                        .orElseGet(() -> ResponseEntity.notFound().build()));
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<?>> create(@RequestParam("file") MultipartFile file) throws URISyntaxException, IOException {
        final String contentType = file.getContentType();
        final String name = file.getName();
        final byte[] bytes = file.getBytes();
        return storageService.create(name, contentType, bytes)
                .thenApply(oc -> oc
                        .map(StorageController::createCreatedResponse)
                        .orElseGet(() -> ResponseEntity.badRequest().build()));
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
