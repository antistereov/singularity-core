package io.stereov.singularity.core.group.model

import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import kotlinx.serialization.Serializable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "groups")
@Serializable
data class GroupDocument(
    @Id private val _id: String?,
    @Indexed(unique = true) val key: String,
    val name: String,
    val description: String,
) {

    val id: String
        get() = _id ?: throw InvalidDocumentException("GroupDocument does not contain ID")
}
