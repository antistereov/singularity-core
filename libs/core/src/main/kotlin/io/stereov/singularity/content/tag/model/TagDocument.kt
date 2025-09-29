package io.stereov.singularity.content.tag.model

import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.translate.model.Translatable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(collection = "tags")
data class TagDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) var key: String,
    override val translations: MutableMap<Locale, TagTranslation> = mutableMapOf(),
) : Translatable<TagTranslation> {

    val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("TagDocument contains no ID")
}
