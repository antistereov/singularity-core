package io.stereov.singularity.content.tag.mapper

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import io.stereov.singularity.content.tag.dto.CreateTagMultiLangRequest
import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.content.tag.model.TagDocument
import io.stereov.singularity.content.tag.model.TagTranslation
import io.stereov.singularity.translate.exception.TranslateException
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.stereotype.Component
import java.util.*

/**
 * A mapper class responsible for handling operations related to tag transformations
 * and creating tag documents with support for translations.
 *
 * This class interacts with a translation service to provide localized responses or create
 * multi-language tag documents based on the inputs provided.
 */
@Component
class TagMapper(
    private val translationService: TranslateService
) {

    /**
     * Creates a response representation of a tag by translating its content to the specified locale.
     *
     * This function leverages the translation service to generate a localized representation
     * of a tag. If the specified locale is provided, it attempts to create a translation
     * based on that locale. If a translation for the provided locale is not available, it
     * may throw a [TranslateException.NoTranslations].
     *
     * @param tag The tag document to be translated and transformed into a response object.
     * @param locale The optional locale to be used for translation. If null, the translation
     *               may fall back to a default or available locale.
     * @return A [Result] wrapping a successfully translated [TagResponse], or an exception
     *  of type [TranslateException.NoTranslations] if no translations are available.
     */
    suspend fun createTagResponse(tag: TagDocument, locale: Locale?): Result<TagResponse, TranslateException.NoTranslations> {

        return translationService.translate(tag, locale)
            .map { translation ->
                TagResponse(
                    key = tag.key,
                    locale = translation.locale,
                    name = translation.translation.name,
                    description = translation.translation.description
                )
            }
    }

    /**
     * Creates a new tag document based on the provided request.
     *
     * This method uses the locale from the request or falls back to the default
     * locale provided by the translation service. It creates a new `TagDocument` instance
     * with the necessary translations for the specified locale.
     *
     * @param req The request containing information required to create the tag,
     * including key, locale, name, and description.
     * @return The created [TagDocument] with the specified key and initial translations.
     */
    fun createTag(req: CreateTagRequest): TagDocument {
        val locale = req.locale ?: translationService.defaultLocale

        return TagDocument(
            _id = null,
            key = req.key,
            translations = mutableMapOf(locale to TagTranslation(req.name, req.description ?: "")),
        )
    }

    /**
     * Creates a new tag document using the provided multi-language tag creation request.
     *
     * This function constructs a [TagDocument] object with the provided key and translations
     * from the [CreateTagMultiLangRequest].
     *
     * @param req The request containing the key and translations for the tag.
     * @return The created [TagDocument] with the specified attributes.
     */
    fun createTag(req: CreateTagMultiLangRequest): TagDocument {
        return TagDocument(
            _id = null,
            key = req.key,
            translations = req.translations,
        )
    }
}
