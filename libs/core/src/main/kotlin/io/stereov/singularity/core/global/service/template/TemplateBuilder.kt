package io.stereov.singularity.core.global.service.template

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.language.model.Language
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.core.io.ClassPathResource
import java.util.*

class TemplateBuilder private constructor(private val template: String) {

    fun build() = template

    suspend fun translate(translationResource: String, lang: Language): TemplateBuilder = withContext(
        Dispatchers.IO) {
        logger.debug { "Translating template to $lang" }

        val bundle = ResourceBundle.getBundle(translationResource, lang.toLocale())
        val regex = Regex("""\{\{\s*([a-zA-Z0-9_.-]+)\s*\|\s*translate\s*}}""")

        val translatedTemplate = regex.replace(template) { matchResult ->
            val key = matchResult.groupValues[1]

            val translation = try { bundle.getString(key) } catch(_: Throwable) { key }

            translation
        }

        return@withContext TemplateBuilder(translatedTemplate)
    }

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

        suspend fun fromResource(templateResource: String): TemplateBuilder = withContext(Dispatchers.IO) {
            logger.debug { "Loading template from resource \"$templateResource\"" }

            val resource = ClassPathResource(templateResource)
            return@withContext TemplateBuilder(resource.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        }
    }
}
