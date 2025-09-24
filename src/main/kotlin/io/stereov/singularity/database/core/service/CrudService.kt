package io.stereov.singularity.database.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.bson.types.ObjectId
import org.springframework.data.domain.*
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface CrudService<T: Any> {

    val logger: KLogger

    val collectionClazz: Class<T>
    val reactiveMongoTemplate: ReactiveMongoTemplate
    val repository: CoroutineCrudRepository<T, ObjectId>

    suspend fun findByIdOrNull(id: ObjectId): T? {
        logger.debug { "Finding ${collectionClazz.name} by ID $id" }

        return repository.findById(id)
    }

    suspend fun findById(id: ObjectId): T {
        return findByIdOrNull(id) ?: throw DocumentNotFoundException("No ${collectionClazz.name} with ID $id found")
    }

    suspend fun existsById(id: ObjectId): Boolean {
        logger.debug { "Checking if ${collectionClazz.name} exists by ID $id" }

        return repository.existsById(id)
    }

    @Suppress("UNUSED")
    suspend fun deleteById(id: ObjectId) {
        logger.debug { "Deleting ${collectionClazz.name} by ID $id" }

        return repository.deleteById(id)
    }

    suspend fun deleteAll() {
        logger.debug { "Deleting all ${collectionClazz.name}" }

        return repository.deleteAll()
    }

    suspend fun save(doc: T): T {
        logger.debug { "Saving ${collectionClazz.name}" }

        return repository.save(doc)
    }

    @Suppress("UNUSED")
    suspend fun saveAll(docs: Collection<T>): List<T> {
        logger.debug { "Saving multiple ${collectionClazz.name}s" }

        return repository.saveAll(docs).toList()
    }

    @Suppress("UNUSED")
    suspend fun findAllPaginated(page: Int, size: Int, sort: List<String>, criteria: Criteria? = null): Page<T> {

        val pageable = PageRequest.of(page, size, Sort.by(sort.map { item ->
            val (property, direction) = item.split(",")
            Sort.Order(Sort.Direction.fromString(direction), property)
        }))

        return findAllPaginated(pageable, criteria)
    }

    suspend fun findAllPaginated(pageable: Pageable, criteria: Criteria? = null): Page<T> {
        logger.debug { "Finding ${collectionClazz.simpleName}: page ${pageable.pageNumber}, size: ${pageable.pageSize}, sort: ${pageable.sort}" }


        val query = criteria?.let { Query(it) } ?: Query()
        val paginatedQuery = query.with(pageable)

        val count = reactiveMongoTemplate.count(paginatedQuery, collectionClazz).awaitFirstOrElse { 0 }
        val groups = reactiveMongoTemplate.find(paginatedQuery, collectionClazz).collectList().awaitFirstOrElse { emptyList() }

        return PageImpl(groups, pageable, count)
    }
}
