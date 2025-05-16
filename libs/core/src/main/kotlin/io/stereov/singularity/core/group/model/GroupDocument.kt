package io.stereov.singularity.core.group.model

import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "groups")
data class GroupDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) val key: String,
    val name: String,
    val description: String = "",
) {

    val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("GroupDocument does not contain ID")
}
