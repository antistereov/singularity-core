package io.stereov.singularity.translate.service

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.exception.TranslateException
import io.stereov.singularity.translate.model.Translatable
import io.stereov.singularity.translate.model.TranslateKey
import io.stereov.singularity.translate.model.Translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service responsible for providing translations for keys or translatable objects,
 * supporting fallback to default locales or available translations.
 *
 * Handles translations using resource bundles or direct mappings for translatable entities.
 */
@Service
class TranslateService(
    appProperties: AppProperties
) {

    val defaultLocale = appProperties.locale
    private val logger = KotlinLogging.logger {}

    /**
     * Translates a given resource key using a specified resource bundle and locale.
     *
     * If a specific locale is provided, it attempts to locate the translation for that locale.
     * If the exact locale translation is not found, it falls back to translations for the
     * language portion of the locale. If no locale is provided, the default locale is used.
     * If no matching translation is available, the original resource key is returned.
     *
     * @param translateKey The translation key to look up in the resource bundle.
     * @param resource The name of the resource file containing the translations.
     * @param locale The locale to use for the translation. If null, the default locale is applied.
     * @return The translated string if available, or the original translation key if no translation is found.
     */
    suspend fun translateResourceKey(
        translateKey: TranslateKey,
        resource: String,
        locale: Locale?
    ): String = withContext(Dispatchers.IO) {
        logger.debug { "Translating key \"${translateKey.key}\" to $locale using resource: \"$resource\"" }

        return@withContext locale
            ?.let { locale ->
                runCatching {
                    val bundle = ResourceBundle.getBundle(resource, locale)
                    bundle.getString(translateKey.key).trim()
                }.getOrElse {
                    runCatching {
                        val bundle = ResourceBundle.getBundle(resource, Locale.forLanguageTag(locale.language))
                        bundle.getString(translateKey.key).trim()
                    }.getOrNull()
                }
            }
            ?: runCatching {
                val bundle = ResourceBundle.getBundle(resource, defaultLocale)
                bundle.getString(translateKey.key).trim()
            }.getOrElse { translateKey.key }
    }

    /**
     * Translates a given translatable object to the specified language or locale.
     *
     * Attempts to find the best matching translation for the given language tag. If the language tag
     * is null or no matching translation is found, it falls back to other available translations
     * or a default locale if applicable.
     *
     * @param translatable The object containing the translations mapped by locale.
     * @param languageTag The IETF BCP 47 language tag specifying the desired language or locale to translate to.
     *                     If null, translation may fall back to a default locale or an available translation.
     * @return A [Result] containing either the matching translation wrapped in [Translation],
     *   or an exception of type [TranslateException.NoTranslations] if no translations are found.
     */
    suspend fun <C> translate(translatable: Translatable<C>, languageTag: String?): Result<Translation<C>, TranslateException.NoTranslations> {
        return translatable.findBestMatchingTranslation(Locale.forLanguageTag(languageTag))
    }

    /**
     * Translates a given translatable object to the specified locale, if available.
     *
     * Attempts to find the best matching translation for the specified locale. If the locale is
     * null or no matching translation is found, it may fall back to other available translations
     * or a default locale, if applicable.
     *
     * @param translatable The object containing the translations mapped by locale.
     * @param locale The desired locale to use for the translation. If null, translation may fall back
     *               to a default locale or available translations.
     * @return A [Result] containing a successful [Translation] of type [C] for the given locale,
     *   or a [TranslateException.NoTranslations] if no translations are available.
     */
    suspend fun <C> translate(translatable: Translatable<C>, locale: Locale?): Result<Translation<C>, TranslateException.NoTranslations> {
        return translatable.findBestMatchingTranslation(locale)
    }

    /**
     * Attempts to find the best matching translation for the specified locale.
     *
     * The method checks if a translation exists for the exact locale. If not, it tries
     * to find a translation for the language component of the locale. If no such
     * translations are found, it falls back to the default locale's translation or the
     * first available translation. If no translations are available, it returns an exception.
     *
     * @param locale The desired locale for the translation. If null, fallback mechanisms
     *               will be applied to find a suitable translation.
     * @return A [Result] containing either the best matching [Translation] or
     *  a [TranslateException.NoTranslations] exception if no translations are found.
     */
    private fun <C> Translatable<C>.findBestMatchingTranslation(locale: Locale?): Result<Translation<C>, TranslateException.NoTranslations> {
        val inputLanguage = locale?.language

        val key = translations.keys.find { it == locale }
            ?: translations.keys.find { it.language == inputLanguage }

        val translationForInput = key?.let { translations[it] }
        if (translationForInput != null) {
            return Ok(Translation(key, translationForInput))
        }

        val translationForDefault = translations[defaultLocale]
        if (translationForDefault != null) {
            return Ok(Translation(defaultLocale, translationForDefault))
        }

        return translations.toList()
            .map { (locale, translation) -> Translation(locale, translation) }
            .firstOrNull()
            .toResultOr { TranslateException.NoTranslations("No translations saved for entity") }
    }
}
