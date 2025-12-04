package io.stereov.singularity.email.template.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import io.stereov.singularity.email.template.exception.TemplateException
import java.util.*

/**
 * Translates a template using the specified translation resource and locale.
 *
 * This method processes a template containing placeholders for translation and replaces those placeholders
 * with the corresponding translated text retrieved from the specified resource bundle for the given locale.
 *
 * @param translationResource The name of the translation resource bundle to be used for retrieving translations.
 * @param locale The locale to be used for fetching translations from the resource bundle.
 * @return A [Result] containing either a [TemplateBuilder] with the translated template or a [TemplateException]
 *    if an error occurs during the translation process.
 */
suspend fun <E: TemplateException> Result<TemplateBuilder, E>.translate(
    translationResource: String,
    locale: Locale
): Result<TemplateBuilder, TemplateException> {
    return this.andThen { builder -> builder.translate(translationResource, locale) }
}

/**
 * Replaces all placeholders in the template with their corresponding values from the provided map.
 * Placeholders are identified using the format `{{ key }}`.
 *
 * @param placeholders A map where keys represent placeholder names, and values are the corresponding
 * values to replace in the template. If a placeholder in the template does not exist in the map, it
 * remains unchanged.
 * @return A new instance of [TemplateBuilder] containing the template with replaced placeholders.
 */
fun <E: TemplateException> Result<TemplateBuilder, E>.replacePlaceholders(placeholders: Map<String, Any>): Result<TemplateBuilder, E> {
    return this.map { builder -> builder.replacePlaceholders(placeholders) }
}

fun <E: TemplateException> Result<TemplateBuilder, E>.build(): Result<String, E> {
    return this.map { builder -> builder.build() }
}