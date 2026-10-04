package tutorial.images.warehouse.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tutorial.images.warehouse.dto.ImageInfo;
import tutorial.images.warehouse.service.StorageService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
public class StorageControllerUnitTest {

    @Mock
    private StorageService storageService;

    @InjectMocks
    private StorageController storageController;

    @Test
    void imageList_OK() throws ExecutionException, InterruptedException {
        doReturn(CompletableFuture.completedFuture(Optional.of(List.of(
                new ImageInfo("one"),
                new ImageInfo("one-1"),
                new ImageInfo("one-2")
        ))))
                .when(storageService)
                .imageList();
        final List<ImageInfo> actual = storageController.imageList()
                .get();
        Assertions.assertEquals(3, actual.size());
        Assertions.assertEquals("one", actual.getFirst().id());
        Assertions.assertEquals("one-1", actual.get(1).id());
        Assertions.assertEquals("one-2", actual.getLast().id());
    }
}
