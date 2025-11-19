package io.stereov.singularity.email.template.util

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.email.template.exception.TemplateException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.ClassPathResource
import java.util.*

/**
 * A utility class for building, processing, and translating templates. The class is designed
 * to process templates containing placeholders and provide various operations, such as
 * replacing placeholders with values and translating templates using resource bundles.
 *
 * Instances of this class can only be created through the provided static methods.
 */
class TemplateBuilder private constructor(private val template: String) {

    fun build() = template

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
    suspend fun translate(
        translationResource: String,
        locale: Locale
    ): Result<TemplateBuilder, TemplateException> = withContext(Dispatchers.IO) {
        logger.debug { "Translating template to $locale" }

        runCatching {
            val bundle = ResourceBundle.getBundle(translationResource, locale)
            val regex = Regex("""\{\{\s*([a-zA-Z0-9_.-]+)\s*\|\s*translate\s*}}""")

            val translatedTemplate = regex.replace(template) { matchResult ->
                val key = matchResult.groupValues[1]

                val translation = try { bundle.getString(key) } catch(_: Throwable) { key }

                translation
            }

            TemplateBuilder(translatedTemplate)
        }.mapError { ex -> when (ex) {
            is TemplateException -> ex
            else -> TemplateException.TranslationBundleNotFound(
                msg = "Failed to load translation bundle $translationResource",
                cause = ex
            )
        } }
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
    fun replacePlaceholders(placeholders: Map<String, Any>): TemplateBuilder {
        logger.debug { "Replacing placeholders in template" }

        val regex = Regex("""\{\{\s*([a-zA-Z0-9_.-]+)\s*}}""")

        val newTemplate = regex.replace(template) { match ->
            val key = match.groupValues[1]
            placeholders[key]?.toString() ?: match.value
        }

        return TemplateBuilder(newTemplate)
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Loads a template from the specified resource file located in the classpath
         * and creates a [TemplateBuilder] instance for further operations on the template.
         *
         * @param templateResource The name or path of the template resource file to be loaded.
         * @return A [Result] containing either a [TemplateBuilder] instance if the resource is successfully loaded,
         * or a [TemplateException.ResourceNotFound] if the resource cannot be found or accessed.
         */
        suspend fun fromResource(
            templateResource: String
        ): Result<TemplateBuilder, TemplateException.ResourceNotFound> = withContext(Dispatchers.IO) {
            logger.debug { "Loading template from resource \"$templateResource\"" }

            runCatching {
                val resource = ClassPathResource(templateResource)
                val content = resource.inputStream.bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                TemplateBuilder(content)
            }.mapError { ex ->
                TemplateException.ResourceNotFound(
                    msg = "Template resource $templateResource not found: ${ex.message}",
                    cause = ex
                )
            }
        }

        /**
         * Creates a new instance of [TemplateBuilder] from the provided template string.
         *
         * @param template The template content in string format which will be used to initialize the [TemplateBuilder].
         * @return A [TemplateBuilder] instance initialized with the given template string.
         */
        fun fromString(template: String): TemplateBuilder {
            logger.debug { "Creating template from String" }

            return TemplateBuilder(template)
        }
    }
}
