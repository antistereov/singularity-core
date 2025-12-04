package io.stereov.singularity.database.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.stereov.singularity.database.core.exception.DeleteDocumentByKeyException
import io.stereov.singularity.database.core.exception.ExistsDocumentByKeyException
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.database.core.model.WithKey
import io.stereov.singularity.database.core.repository.CoroutineCrudRepositoryWithKey

interface CrudServiceWithKey<D: WithKey> : CrudService<D> {

    override val repository: CoroutineCrudRepositoryWithKey<D>

    /**
     * Suspends and retrieves a group document by the provided key.
     *
     * @param key The unique key identifying the group to be retrieved.
     * @return A [Result] containing the Document if found, or a [FindDocumentByKeyException]
     *  detailing the failure reason if the operation is unsuccessful.
     */
    suspend fun findByKey(key: String): Result<D, FindDocumentByKeyException> {
        logger.debug { "Finding ${collectionClazz.simpleName} by key \"$key\"" }

        return runSuspendCatching { repository.findByKey(key) }
            .mapError { ex -> FindDocumentByKeyException.Database("Failed to check existence of ${collectionClazz.simpleName} with key $key: ${ex.message}", ex) }
            .andThen { it.toResultOr { FindDocumentByKeyException.NotFound("No ${collectionClazz.simpleName} with key \"$key\" found") }}
    }

    /**
     * Checks if a group with the specified key exists in the database.
     *
     * @param key The unique key to check for existence in the database.
     * @return A [Result] containing true if the group exists, false otherwise.
     *  Returns a failure with [ExistsDocumentByKeyException] in case of an error.
     */
    suspend fun existsByKey(key: String): Result<Boolean, ExistsDocumentByKeyException> {
        logger.debug { "Checking if ${collectionClazz.simpleName} with key \"$key\" exists" }

        return runSuspendCatching { repository.existsByKey(key) }
            .mapError { ex -> ExistsDocumentByKeyException.Database("Failed to check existence of ${collectionClazz.simpleName} with key $key: ${ex.message}", ex) }
    }

    /**
     * Deletes a document identified by the specified key from the database.
     *
     * This function attempts to delete a document by its unique key. If the operation
     * succeeds, it returns a unit result. If the deletion fails due to a database error,
     * an appropriate exception is returned.
     *
     * @param key The unique key identifying the document to be deleted.
     * @return A [Result] containing [Unit] if the deletion is successful, or
     * a [DeleteDocumentByKeyException] indicating the failure reason.
     */
    suspend fun deleteByKey(key: String): Result<Unit, DeleteDocumentByKeyException> = coroutineBinding {
        logger.debug { "Deleting ${collectionClazz.simpleName} with key \"$key\"" }

        val exists = existsByKey(key)
            .mapError { ex -> DeleteDocumentByKeyException.Database("Failed to check existence of ${collectionClazz.simpleName} with key $key: ${ex.message}", ex) }
            .bind()

        if (exists) {
            runSuspendCatching { repository.deleteByKey(key) }
                .mapError { ex ->
                    DeleteDocumentByKeyException.Database(
                        "Failed to delete ${collectionClazz.simpleName} with key $key: ${ex.message}",
                        ex
                    )
                }
                .bind()
        } else {
            Err(DeleteDocumentByKeyException.NotFound("No ${collectionClazz.simpleName} with key $key found"))
                .bind()
        }
    }

}