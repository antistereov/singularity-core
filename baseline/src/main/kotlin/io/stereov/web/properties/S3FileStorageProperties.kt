package io.stereov.web.properties

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.regions.Region

@ConfigurationProperties("baseline.file.storage.s3")
@ConditionalOnProperty(prefix = "baseline.file.storage", name = ["type"], havingValue = "s3", matchIfMissing = false)
data class S3FileStorageProperties(
    val uri: String = "http://localhost:9000",
    val bucket: String = "app",
    val region: Region = Region.EU_CENTRAL_1,
    val accessKey: String,
    val secretKey: String,
)
