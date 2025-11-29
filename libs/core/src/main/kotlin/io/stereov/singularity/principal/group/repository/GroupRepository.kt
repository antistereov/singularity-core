package io.stereov.singularity.principal.group.repository

import io.stereov.singularity.database.core.repository.CoroutineCrudRepositoryWithKey
import io.stereov.singularity.principal.group.model.Group
import org.springframework.stereotype.Repository

@Repository
interface GroupRepository : CoroutineCrudRepositoryWithKey<Group>
