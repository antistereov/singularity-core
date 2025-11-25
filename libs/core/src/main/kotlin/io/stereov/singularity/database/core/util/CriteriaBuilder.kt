package io.stereov.singularity.database.core.util

import io.stereov.singularity.translate.model.Translatable
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import java.util.*
import kotlin.reflect.KProperty

class CriteriaBuilder(
    val criteriaList: MutableList<Criteria> = mutableListOf()
) {

    constructor(criteria: Criteria): this(mutableListOf(criteria))

    fun build(): Criteria? {
        return if (criteriaList.isNotEmpty()) {
            Criteria().andOperator(*criteriaList.toTypedArray())
        } else null
    }

    fun query(): Query {
        return build()?.let { Query(it) } ?: Query()
    }

    private fun getFieldName(field: String, locale: Locale? = null) = when(locale) {
        null -> field
        else -> "${Translatable<*>::translations.name}.$locale.$field"
    }

    fun fieldContains(field: String, substring: String?, locale: Locale? = null): CriteriaBuilder {
        if (substring == null) return this

        val actualField = getFieldName(field, locale)
        val regexPattern = ".*${Regex.escape(substring)}.*"
        criteriaList.add(Criteria.where(actualField).regex(regexPattern, "i"))

        return this
    }
    fun fieldContains(field: KProperty<*>, substring: String?, locale: Locale? = null): CriteriaBuilder {
        return fieldContains(field.name, substring, locale)
    }

    fun compare(field: String, lte: Comparable<*>?, gte: Comparable<*>?, equals: Comparable<*>? = null, locale: Locale? = null): CriteriaBuilder {
        val actualField = getFieldName(field, locale)

        lte?.let { criteriaList.add(Criteria.where(actualField).lte(lte)) }
        gte?.let { criteriaList.add(Criteria.where(actualField).gte(gte)) }
        equals?.let { criteriaList.add(Criteria.where(actualField).isEqualTo(equals))}

        return this
    }
    fun compare(field: KProperty<*>, lte: Comparable<*>?, gte: Comparable<*>?, equals: Comparable<*>? = null, locale: Locale? = null): CriteriaBuilder {
        return compare(field.name, lte, gte, equals, locale)
    }
    fun isEqualTo(field: String, value: Any?, locale: Locale? = null): CriteriaBuilder {
        if (value == null) return this

        val actualField = getFieldName(field, locale)
        criteriaList.add(Criteria.where(actualField).isEqualTo(value))

        return this
    }
    fun isEqualTo(field: KProperty<*>, value: Any?, locale: Locale? = null): CriteriaBuilder {
        return isEqualTo(field.name, value, locale)
    }
    fun isIn(field: String, collection: Collection<*>?, locale: Locale? = null): CriteriaBuilder {
        if (collection.isNullOrEmpty()) return this
        criteriaList.add(Criteria.where(getFieldName(field, locale)).`in`(collection))

        return this
    }
    fun isIn(field: KProperty<*>, collection: Collection<*>?, locale: Locale? = null): CriteriaBuilder {
        return isIn(field.name, collection, locale)
    }
    fun hasElement(field: String, value: Any?, locale: Locale? = null): CriteriaBuilder {
        if (value == null) return this
        criteriaList.add(Criteria.where(getFieldName(field, locale)).`is`(value))

        return this
    }
    fun hasElement(field: KProperty<*>, value: Any?, locale: Locale? = null): CriteriaBuilder {
        return hasElement(field.name, value, locale)
    }
    fun existsAny(values: Collection<*>?, prefix: String? = null, locale: Locale? = null): CriteriaBuilder {
        if (values.isNullOrEmpty()) return this
        val actualPrefix = prefix?.let { it.removeSuffix(".") + "." }
        val criteria = Criteria().orOperator(
            *values.map { value ->
                val actualField = getFieldName("${actualPrefix ?: ""}$value", locale)
                Criteria.where(actualField).exists(true)
            }.toTypedArray()
        )

        criteriaList.add(criteria)
        return this
    }
    fun existsAny(values: Collection<*>?, prefix: KProperty<*>? = null, locale: Locale? = null): CriteriaBuilder {
        return existsAny(values, prefix?.name, locale)
    }

    fun add(criteria: Criteria): CriteriaBuilder {
        criteriaList.add(criteria)
        return this
    }

}