package io.stereov.singularity.database.core.model

import org.bson.types.ObjectId

interface WithId {
    val id: ObjectId
}