package io.stereov.singularity.database.core.model

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import io.stereov.singularity.database.core.exception.DocumentException
import org.bson.types.ObjectId

interface WithId {
    val _id: ObjectId?

    val id: Result<ObjectId, DocumentException.Invalid>
        get() = _id.toResultOr { DocumentException.Invalid("Invalid document: contains no ID") }

}