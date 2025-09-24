package io.stereov.singularity.auth.group.model

import io.stereov.singularity.global.exception.model.InvalidDocumentException
import io.stereov.singularity.translate.model.Translatable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(collection = "groups")
data class GroupDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) var key: String,
    override val translations: MutableMap<Locale, GroupTranslation>,
    override var primaryLocale: Locale
) : Translatable<GroupTranslation> {

    val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("GroupDocument does not contain ID")
}
