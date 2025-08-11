package io.stereov.singularity.database.core.service

import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.translate.model.Translatable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import kotlin.reflect.full.memberProperties

interface TranslatableCrudService<C: Any, T: Translatable<C>> : CrudService<T> {

    val contentClass: Class<C>

    private val fieldNames: List<String>
        get() = contentClass.kotlin.memberProperties.map { it.name }

    suspend fun findAllPaginated(page: Int, size: Int, sort: List<String>, criteria: Criteria? = null, lang: Language): Page<T> {

        val pageable = if (sort.isEmpty()) {
            PageRequest.of(page, size)
        } else {
            val property = sort[0]
            val direction = sort.getOrNull(1)?.uppercase() ?: "ASC"
            val actualProperty = if (fieldNames.contains(property)) {
                "${Translatable<*>::translations.name}.$lang.$property"
            } else {
                property
            }

            PageRequest.of(page, size, Sort.by(Sort.Order(Sort.Direction.fromString(direction), actualProperty)))
        }

        return super.findAllPaginated(pageable, criteria)
    }
}
