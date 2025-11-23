package io.stereov.singularity.global.util

import com.github.slugify.Slugify
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.server.reactive.ServerHttpRequest

fun String.toSlug(): String {
    return Slugify.builder().build().slugify(this)
}

fun ServerHttpRequest.getClientIp(preferredHeader: String = "X-Real-IP"): String? {
    return headers[preferredHeader]?.firstOrNull()
        ?: headers["X-Real-IP"]?.firstOrNull()
        ?: headers["X-Forwarded-For"]?.firstOrNull()?.split(",")?.firstOrNull()
        ?: remoteAddress?.address?.hostAddress
}

suspend fun <S, T> Page<T>.mapContent(map: suspend (content: T) -> S): Page<S> {
    val mappedContent = this.content.map { map(it) }
    return PageImpl(mappedContent, this.pageable, this.totalElements)
}

data class GeneratedDocs(
    val securityRequirements: SecurityRequirement,
    val errorResponses: List<ApiResponse>
)
