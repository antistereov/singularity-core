package io.stereov.web.properties.storage


import io.stereov.web.global.model.HttpScheme
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import software.amazon.awssdk.regions.Region

@ConfigurationProperties("baseline.file.storage.s3")
@ConditionalOnProperty(prefix = "baseline.file.storage", value = ["type"], havingValue = "s3", matchIfMissing = false)
data class S3Properties(
    val domain: String,
    val scheme: HttpScheme = HttpScheme.HTTP,
    val bucket: String = "app",
    val region: Region = Region.EU_CENTRAL_1,
    val accessKey: String,
    val secretKey: String,
    val signatureDuration: Long = 5
)
