package io.stereov.singularity.content.tag.repository

import io.stereov.singularity.content.tag.model.TagDocument
import io.stereov.singularity.database.core.repository.CoroutineCrudRepositoryWithKey
import org.springframework.stereotype.Repository

@Repository
interface TagRepository : CoroutineCrudRepositoryWithKey<TagDocument>
