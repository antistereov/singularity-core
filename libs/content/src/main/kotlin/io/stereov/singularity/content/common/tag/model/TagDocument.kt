package io.stereov.singularity.content.common.tag.model

import io.stereov.singularity.content.common.tag.dto.CreateTagMultiLangRequest
import io.stereov.singularity.content.common.tag.dto.CreateTagRequest
import io.stereov.singularity.content.common.tag.dto.TagResponse
import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.global.language.model.Translatable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "tags")
data class TagDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) var key: String,
    override val translations: MutableMap<Language, TagTranslation> = mutableMapOf(),
) : Translatable<TagTranslation> {

    override val primaryLanguage: Language = Language.EN

    constructor(req: CreateTagRequest): this(
        _id = null,
        key = req.key,
        translations = mutableMapOf(req.lang to TagTranslation(req.name, req.description ?: "")),
    )

    constructor(req: CreateTagMultiLangRequest): this (
        _id = null,
        key = req.key,
        translations = req.translations,
    )

    val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("TagDocument contains no ID")


    fun toResponse(lang: Language): TagResponse {
        val (lang, translation) = translate(lang)
        return TagResponse(key, lang, translation.name, translation.description)
    }
}
