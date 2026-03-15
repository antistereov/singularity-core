package io.stereov.singularity.file.core.model

import io.stereov.singularity.database.core.model.WithId

interface WithFileStorage : WithId {
    val prefix: String

    val fileStoragePath: String
        get() = prefix
            .removePrefix("/")
            .removeSuffix("/")
            .plus("/")
            .plus(id.toHexString())
}