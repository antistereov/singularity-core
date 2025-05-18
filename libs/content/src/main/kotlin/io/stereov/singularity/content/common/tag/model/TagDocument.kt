package io.stereov.singularity.content.common.tag.model

import io.stereov.singularity.content.common.tag.dto.CreateTagRequest
import io.stereov.singularity.content.common.tag.dto.TagResponse
import io.stereov.singularity.core.global.exception.model.InvalidDocumentException
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "tags")
data class TagDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) var name: String,
    var description: String,
) {

    constructor(req: CreateTagRequest): this(
        _id = null,
        name = req.name,
        description = req.description ?: ""
    )

    val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("TagDocument contains no ID")

    fun toResponse(): TagResponse {
        return TagResponse(id, name, description)
    }
}
