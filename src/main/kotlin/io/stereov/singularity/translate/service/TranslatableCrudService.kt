package io.stereov.singularity.translate.service

import com.github.michaelbull.result.Result
import io.stereov.singularity.database.core.exception.FindAllDocumentsPaginatedException
import io.stereov.singularity.database.core.model.WithId
import io.stereov.singularity.database.core.service.CrudService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.model.Translatable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import java.util.*
import kotlin.reflect.full.memberProperties

/**
 * A CRUD service interface for entities that implement the [Translatable] interface, adding support for
 * localized sorting and pagination.
 *
 * @param C The type of the content that can be translated.
 * @param T The type of the translatable entity managed by this service.
 */
interface TranslatableCrudService<C: Any, T> : CrudService<T> where T : Translatable<C>, T: WithId  {

    val contentClass: Class<C>
    val appProperties: AppProperties

    private val fieldNames: Set<String>
        get() = contentClass.kotlin.memberProperties.map { it.name }.toSet()

    /**
     * Retrieves a paginated list of entities based on the provided pageable configuration and optional criteria,
     * with support for localized sorting based on the specified locale.
     *
     * @param pageable Configuration for pagination and sorting.
     * @param criteria Optional criteria used to filter the entities.
     * @param locale The locale to be used for resolving field translations. If not provided, a default locale is applied.
     * @return A [Result] containing the paginated [Page] of entities or a [FindAllDocumentsPaginatedException] in case of an error.
     */
    suspend fun findAllPaginated(
        pageable: Pageable,
        criteria: Criteria? = null,
        locale: Locale?
    ): Result<Page<T>, FindAllDocumentsPaginatedException> {

        val actualLocale = locale ?: appProperties.locale
        val actualPageable = pageable.withLocalizedSort(actualLocale, fieldNames)
        return super.findAllPaginated(actualPageable, criteria)
    }

    /**
     * Modifies the sort order of the current Pageable to include localized sorting for translatable fields
     * based on the provided locale.
     *
     * @param locale The locale to be used for generating localized sort fields.
     * @param translatableFields A set of field names that support localization and need to be translated.
     * @return A new Pageable instance with the modified sort configuration including localized fields.
     */
    private fun Pageable.withLocalizedSort(
        locale: Locale,
        translatableFields: Set<String>
    ): Pageable {
        val translatedSort = this.sort.map { order ->
            val originalProperty = order.property
            val actualProperty = if (translatableFields.contains(originalProperty)) {
                "${Translatable<*>::translations.name}.${locale}.$originalProperty"
            } else {
                originalProperty
            }

            Sort.Order(order.direction, actualProperty)
        }

        val newSort = Sort.by(translatedSort.toList())
        return PageRequest.of(this.pageNumber, this.pageSize, newSort)
    }
}