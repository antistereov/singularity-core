package io.stereov.singularity.global.util

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.slugify.Slugify
import io.stereov.singularity.translate.model.Translatable
import org.springframework.data.domain.*
import org.springframework.http.server.reactive.ServerHttpRequest
import java.util.*
import kotlin.reflect.KProperty

fun Pageable.withLocalizedSort(
    locale: Locale,
    translatableProperties: List<KProperty<*>>
): Pageable {
    val translatableFieldNames = translatableProperties.map { it.name }.toSet()

    return this.withLocalizedSort(locale, translatableFieldNames)
}

fun Pageable.withLocalizedSort(
    locale: Locale,
    translatableFields: Set<String>
): Pageable {
    val translatedSort = this.sort.map { order ->
        val originalProperty = order.property
        val actualProperty = if (translatableFields.contains(originalProperty)) {
            "${Translatable<*>::translations.name}.${locale}.$originalProperty"
        } else {
            originalProperty
        }

        Sort.Order(order.direction, actualProperty)
    }

    val newSort = Sort.by(translatedSort.toList())
    return PageRequest.of(this.pageNumber, this.pageSize, newSort)
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

suspend fun <S, T> Page<T>.mapContent(map: suspend (content: T) -> S): Page<S> {
    val mappedContent = this.content.map { map(it) }
    return PageImpl(mappedContent, this.pageable, this.totalElements)
}

inline fun <E, T> catchAs(block: () -> T, onError: (Throwable) -> E): Result<T, E> =
    try { Ok(block()) } catch (t: Throwable) { Err(onError(t)) }

