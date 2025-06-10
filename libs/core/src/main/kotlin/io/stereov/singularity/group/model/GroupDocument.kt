package io.stereov.singularity.group.model

import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.group.dto.CreateGroupRequest
import io.stereov.singularity.group.dto.GroupResponse
import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.translate.model.Translatable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "groups")
data class GroupDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) val key: String,
    override val translations: MutableMap<Language, GroupTranslation>,
    override val primaryLanguage: Language = Language.EN
) : Translatable<GroupTranslation> {


    constructor(req: CreateGroupRequest): this(
        key = req.key,
        translations = req.translations
    )

    val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("GroupDocument does not contain ID")

    fun toResponse(lang: Language): GroupResponse {
        val (translatedLang, content) = translate(lang)

        return GroupResponse(
            key = key,
            lang = translatedLang,
            name = content.name,
            description = content.description
        )
    }
}
