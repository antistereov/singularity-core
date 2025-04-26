package io.stereov.singularity.config.storage

import io.stereov.singularity.global.service.file.service.S3FileStorage
import io.stereov.singularity.properties.AppProperties
import io.stereov.singularity.properties.storage.S3Properties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@AutoConfiguration(
    after = [
        StorageConfiguration::class,
    ]
)
@EnableConfigurationProperties(S3Properties::class)
@ConditionalOnProperty(prefix = "baseline.file.storage", value = ["type"], havingValue = "s3", matchIfMissing = false)
class S3Configuration {

    @Bean
    @ConditionalOnMissingBean
    fun s3Client(s3Properties: S3Properties): S3AsyncClient {
        return S3AsyncClient.builder()
            .endpointOverride(URI.create("${s3Properties.scheme}${s3Properties.domain}"))
            .region(s3Properties.region)
            .credentialsProvider(
               StaticCredentialsProvider.create(
                   AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey)
               )
            )
            .overrideConfiguration(
                ClientOverrideConfiguration.builder()
                    .build()
            )
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun s3Presigner(s3Properties: S3Properties): S3Presigner {
        return S3Presigner.builder()
            .endpointOverride(URI.create(s3Properties.domain))
            .region(s3Properties.region)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3Properties.accessKey, s3Properties.secretKey)
            ))
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun fileStorage(s3Properties: S3Properties, s3AsyncClient: S3AsyncClient, s3Presigner: S3Presigner, appProperties: AppProperties): S3FileStorage {
        return S3FileStorage(s3Properties, s3AsyncClient, s3Presigner, appProperties)
    }
}
