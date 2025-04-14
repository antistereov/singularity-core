package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.regions.Region

@ConfigurationProperties("baseline.file.storage.s3")
data class S3Properties(
    val uri: String,
    val bucket: String = "app",
    val region: Region = Region.EU_CENTRAL_1,
    val accessKey: String,
    val secretKey: String,
    val signatureDuration: Long = 5
)
