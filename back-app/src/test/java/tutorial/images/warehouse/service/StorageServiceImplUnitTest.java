package tutorial.images.warehouse.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import tutorial.images.warehouse.config.AwsProperties;
import tutorial.images.warehouse.dto.ImageContent;
import tutorial.images.warehouse.dto.ImageInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorageServiceImplUnitTest {

    @Mock
    private S3AsyncClient s3AsyncClient;

    @Mock
    private AwsProperties awsProperties;

    @Mock
    private ListObjectsResponse listObjectsResponse;

    @Mock
    private HeadObjectResponse headObjectResponse;

    @Mock
    private GetObjectResponse getObjectResponse;

    @InjectMocks
    private StorageServiceImpl storageService;

    @Test
    void getList_OK() throws ExecutionException, InterruptedException {

        final S3Object s3object1 = S3Object.builder()
                .key("full-1")
                .build();
        final S3Object s3object2 = S3Object.builder()
                .key("thumbnail-1")
                .build();
        final S3Object s3object3 = S3Object.builder()
                .key("full-2")
                .build();
        doReturn(List.of(s3object1, s3object2, s3object3))
                .when(listObjectsResponse)
                .contents();
        doReturn(CompletableFuture.completedFuture(listObjectsResponse))
                .when(s3AsyncClient)
                .listObjects(any(ListObjectsRequest.class));

        final List<ImageInfo> actual = storageService.imageList()
                .get()
                .orElseThrow();

        Assertions.assertEquals(2, actual.size());
        Assertions.assertEquals("1", actual.getFirst().id());
        Assertions.assertEquals("2", actual.getLast().id());
        verify(awsProperties).s3bucket();
    }

    @Test
    void getList_AwsError() throws ExecutionException, InterruptedException {
        doReturn(CompletableFuture.failedFuture(new RuntimeException("some AWS exception")))
                .when(s3AsyncClient)
                .listObjects(any(ListObjectsRequest.class));

        Assertions.assertTrue(storageService.imageList().get().isEmpty());
        verify(awsProperties).s3bucket();
    }

    @Test
    void imageContentThumbnail_OK() throws ExecutionException, InterruptedException {
        doReturn(CompletableFuture.completedFuture(null))
                .when(s3AsyncClient)
                .headObject(any(HeadObjectRequest.class));

        doReturn("image/jpeg")
                .when(getObjectResponse)
                .contentType();

        final byte[] content = {1, 2, 3};

        final ResponseBytes<GetObjectResponse> responseBytes =
                ResponseBytes.fromByteArray(getObjectResponse, content);

        doReturn(CompletableFuture.completedFuture(responseBytes))
                .when(s3AsyncClient)
                .getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));

        final ImageContent actual = storageService.imageContentThumbnail("id-1")
                .get()
                .orElseThrow();

        Assertions.assertEquals("image/jpeg", actual.type());
        Assertions.assertArrayEquals(content, actual.data());

        verify(s3AsyncClient).headObject(any(HeadObjectRequest.class));
        verify(s3AsyncClient).getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class));
    }

}
