package io.stereov.singularity.translate.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.model.Translatable
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.model.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class TranslateService(
    private val appProperties: AppProperties
) {

    val defaultLocale = appProperties.locale

    private val logger = KotlinLogging.logger {}

    suspend fun translateResourceKey(
        translateKey: TranslateKey,
        resource: String,
        locale: Locale?
    ): String = withContext(Dispatchers.IO) {
        logger.debug { "Translating key \"${translateKey.key}\" to $locale using resource: \"$resource\"" }

        val actualLocale = locale ?: appProperties.locale

        return@withContext try {
            val bundle = ResourceBundle.getBundle(resource, actualLocale)
            bundle.getString(translateKey.key).trim()
        } catch (_: Throwable) {
            val bundle = ResourceBundle.getBundle(resource, actualLocale)
            bundle.getString(translateKey.key).trim()
        } catch (_: Throwable) {
            return@withContext translateKey.key
        }
    }

    suspend fun <C> translate(translatable: Translatable<C>, languageTag: String?): Translation<C> {
        return translatable.translate(Locale.forLanguageTag(languageTag))
    }

    suspend fun <C> translate(translatable: Translatable<C>, locale: Locale?): Translation<C> {
        return translatable.translate(locale)
    }

    private fun <C> Translatable<C>.translate(locale: Locale?): Translation<C> {
        return locale
            ?.let { findBestMatchingTranslation(it) }
            ?: getDefaultTranslation()
    }

    private fun <C> Translatable<C>.findBestMatchingTranslation(locale: Locale): Translation<C>? {
        val inputLanguage = locale.language

        val key = translations.keys.find { it == locale }
            ?: translations.keys.find { it.language == inputLanguage }

        val translation = key?.let { translations[it] }
        return translation?.let { Translation(key, translation) }
    }

    private fun <C> Translatable<C>.getDefaultTranslation(): Translation<C> {

        return findBestMatchingTranslation(appProperties.locale)
            ?: findBestMatchingTranslation(primaryLocale)
            ?: throw DocumentNotFoundException("The requested item does not contain any translations")
    }
}
