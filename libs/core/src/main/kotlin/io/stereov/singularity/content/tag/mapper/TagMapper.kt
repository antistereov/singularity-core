package io.stereov.singularity.content.tag.mapper

import io.stereov.singularity.content.tag.dto.CreateTagMultiLangRequest
import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.content.tag.model.TagDocument
import io.stereov.singularity.content.tag.model.TagTranslation
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.stereotype.Component
import java.util.*

@Component
class TagMapper(
    private val translationService: TranslateService
) {

    suspend fun createTagResponse(tag: TagDocument, locale: Locale?): TagResponse {

        val tagTranslation = translationService.translate(tag, locale)

        return TagResponse(
            key = tag.key,
            locale = tagTranslation.locale,
            name = tagTranslation.translation.name,
            description = tagTranslation.translation.description
        )
    }

    fun createTag(req: CreateTagRequest): TagDocument {
        val locale = req.locale ?: translationService.defaultLocale

        return TagDocument(
            _id = null,
            key = req.key,
            translations = mutableMapOf(locale to TagTranslation(req.name, req.description ?: "")),
            primaryLocale = locale
        )
    }

    fun createTag(req: CreateTagMultiLangRequest): TagDocument {
        return TagDocument(
            _id = null,
            key = req.key,
            translations = req.translations,
            primaryLocale = req.primaryLocale
        )
    }
}
