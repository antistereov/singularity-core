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

/**
 * Executes the specified [action] after processing either the success or failure result.
 * The provided [action] is called regardless of the outcome of the [Result].
 *
 * @param action A lambda function that will be executed after the result is processed.
 * @return The original [Result] instance.
 */
suspend fun <V, E> Result<V, E>.finally(action: suspend () -> Unit): Result<V,E> {
    action()
    return this
}

/**
 * Applies the specified suspendable [action] to this [Result] instance, regardless of whether it represents
 * a success or failure, and returns the result of executing the [action].
 *
 * @param action A suspendable function that takes the current [Result] as input and returns a transformed [Result].
 * @return A new [Result] transformed by the executed [action].
 */
suspend fun <V, E, W> Result<V, E>.finallyMap(action: suspend (Result<V, E>) -> Result<W, E>): Result<W, E> {
    return action(this)
}
