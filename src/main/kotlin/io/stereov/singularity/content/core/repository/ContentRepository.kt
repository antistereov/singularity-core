package io.stereov.singularity.content.core.repository

import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.database.core.repository.CoroutineCrudRepositoryWithKey

interface ContentRepository<T: ContentDocument<T>> : CoroutineCrudRepositoryWithKey<T>
