package io.stereov.singularity.global.util

import com.github.slugify.Slugify
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.http.server.reactive.ServerHttpRequest

fun getFieldContainsCriteria(field: String, substring: String): Criteria {
    val regexPattern = ".*${Regex.escape(substring)}.*"
    return Criteria.where(field).regex(regexPattern, "i")
}

fun String.toSlug(): String {
    return Slugify.builder().build().slugify(this)
}

fun ServerHttpRequest.getClientIp(preferredHeader: String = "X-Real-IP"): String? {
    return headers[preferredHeader]?.firstOrNull()
        ?: headers["X-Real-IP"]?.firstOrNull()
        ?: headers["X-Forwarded-For"]?.firstOrNull()?.split(",")?.firstOrNull()
        ?: remoteAddress?.address?.hostAddress
}
