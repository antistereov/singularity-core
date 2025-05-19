package io.stereov.singularity.core.group.model

import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.global.language.model.Translatable
import io.stereov.singularity.core.group.dto.CreateGroupMultiLangRequest
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "groups")
data class GroupDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) val key: String,
    override val translations: MutableMap<Language, GroupTranslation>,
) : Translatable<GroupTranslation> {

    override val primaryLanguage: Language = Language.EN

    constructor(req: CreateGroupMultiLangRequest): this(
        key = req.key,
        translations = req.translations
    )

    val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("GroupDocument does not contain ID")
}
