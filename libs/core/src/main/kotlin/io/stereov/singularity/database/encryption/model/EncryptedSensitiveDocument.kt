package io.stereov.singularity.database.encryption.model

import org.bson.types.ObjectId

interface EncryptedSensitiveDocument<T> {

    val _id: ObjectId?
    val sensitive: Encrypted<T>
}
