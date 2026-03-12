package io.stereov.singularity.global.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.github.slugify.Slugify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.server.reactive.ServerHttpRequest

fun String.toSlug(): String {
    return Slugify.builder().build().slugify(this)
}

/**
 * Retrieves the client's IP address from the server HTTP request.
 * It attempts to read the IP address from the provided `preferredHeader`
 * and falls back to common headers such as "X-Real-IP" and "X-Forwarded-For".
 * If no headers contain the IP address, it attempts to obtain it from
 * the remote address.
 *
 * @param preferredHeader The name of the header to prioritize for retrieving the client's IP address.
 *                        Defaults to "X-Real-IP" if not specified.
 * @return The client's IP address as a string, or `null` if it cannot be determined.
 */
fun ServerHttpRequest.getClientIp(preferredHeader: String = "X-Real-IP"): String? {
    return headers[preferredHeader]?.firstOrNull()
        ?: headers["X-Real-IP"]?.firstOrNull()
        ?: headers["X-Forwarded-For"]?.firstOrNull()?.split(",")?.firstOrNull()
        ?: remoteAddress?.address?.hostAddress
}

suspend fun <S: Any, T: Any> Page<T>.mapContent(map: suspend (content: T) -> S): Page<S> {
    val mappedContent = this.content.map { map(it) }
    return PageImpl(mappedContent, this.pageable, this.totalElements)
}

/**
 * Returns the successful value of the [Result] if it is successful, or `null` if it is an error.
 *
 * @return The value of type [V] if the [Result] is a success, otherwise `null`.
 */
fun <V, E> Result<V, E>.getOrNull(): V? {
    return this.getOrElse { null }
}

inline fun <T> T.letIf(
    condition: (T) -> Boolean,
    block: (T) -> T
): T = if (condition(this)) block(this) else this
