package io.stereov.singularity.content.common.tag.repository

import io.stereov.singularity.content.common.tag.model.TagDocument
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TagRepository : CoroutineCrudRepository<TagDocument, ObjectId> {

    suspend fun existsByName(name: String): Boolean
}
