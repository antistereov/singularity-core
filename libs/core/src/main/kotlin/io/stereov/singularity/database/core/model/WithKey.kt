package io.stereov.singularity.database.core.model

interface WithKey : WithId {
    val key: String
}