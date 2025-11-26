package io.stereov.singularity.principal.group.model

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import io.stereov.singularity.database.core.exception.DocumentException
import io.stereov.singularity.translate.model.Translatable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

/**
 * Represents a group entity that can be stored in the database and retrieved by its unique key.
 *
 * A `Group` entity contains translations for different locales and is identified uniquely by its `key`.
 * The `_id` property represents the database identifier for the group and can be null in case
 * the object is not yet persisted. The `id` property provides a safe mechanism to retrieve the `_id`
 * or return an appropriate exception if the document is invalid.
 *
 * @property _id The unique identifier of the group, as stored in the database.
 * @property key A unique string identifier for the group, intended for lookup purposes.
 * @property translations A map containing localized translations of the group’s name and description,
 * where the key is a [Locale] and the value is a [GroupTranslation].
 */
@Document(collection = "groups")
data class Group(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) var key: String,
    override val translations: MutableMap<Locale, GroupTranslation>,
) : Translatable<GroupTranslation> {

    val id: Result<ObjectId, DocumentException.Invalid>
        get() = _id.toResultOr { DocumentException.Invalid("Group document does not contain an ID") }
}
