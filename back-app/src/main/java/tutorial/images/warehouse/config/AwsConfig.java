package tutorial.images.warehouse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class AwsConfig {

    @Bean
    public S3AsyncClient s3AsyncClient(AwsProperties props) {
        return S3AsyncClient.builder()
                .region(Region.of(props.region()))
                .endpointOverride(URI.create(props.endpoint()))
                .credentialsProvider(getCredentialsProvider(props))
                .forcePathStyle(true)
                .build();
    }

    private static StaticCredentialsProvider getCredentialsProvider(AwsProperties props) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        props.accessKey(),
                        props.secretKey()
                )
        );
    }
}
