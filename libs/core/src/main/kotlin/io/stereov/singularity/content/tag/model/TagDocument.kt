package io.stereov.singularity.content.tag.model

import io.stereov.singularity.database.core.model.WithKey
import io.stereov.singularity.translate.model.Translatable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

/**
 * Represents a document for tags in a database collection.
 * Each tag is uniquely identified by its key and can have multiple translations.
 *
 * This class is marked as a MongoDB document and is stored in the "tags" collection.
 * It implements both [Translatable] to handle translations and [WithKey] for key-based identification.
 *
 * @property _id The unique identifier for the document, managed by the database.
 * @property key The unique key for the tag. This field is indexed and must be unique.
 * @property translations A mutable map containing translations of the tag's content for different locales.
 */
@Document(collection = "tags")
data class TagDocument(
    @Id override val _id: ObjectId? = null,
    @Indexed(unique = true) override var key: String,
    override val translations: MutableMap<Locale, TagTranslation> = mutableMapOf(),
) : Translatable<TagTranslation>, WithKey
