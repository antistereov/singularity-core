package io.stereov.singularity.core.global.database.util

import io.stereov.singularity.core.global.database.exception.model.UnexpectedContentTypeException
import io.stereov.singularity.core.global.database.model.PagedResult
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.global.language.model.Translatable
import org.bson.Document
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.query.Criteria

inline fun <reified T> pageFromPagedResult(
    results: List<PagedResult>,
    pageable: Pageable,
    converter: MappingMongoConverter
): Page<T> {
    println(results)

    val content = results.map { result ->
        result.content.map {
            runCatching {
                converter.read(T::class.java, it)
            }.getOrElse { e ->
                throw UnexpectedContentTypeException(T::class.java, e)
            }
        }
    }.flatten()
    val count = results.firstOrNull()?.count?.firstOrNull()?.total ?: 0L

    return PageImpl(content, pageable, count)
}

suspend fun getTranslations(lang: Language, prefix: String? = null): ConditionalOperators.Cond {
    val fallbackLanguage = Language.EN
    val translationsField = Translatable<*>::translations.name
    val primaryLanguage = Translatable<*>::primaryLanguage.name

    val translations = prefix?.let { "$it.$translationsField" } ?: translationsField

    val translationProjection = ConditionalOperators.`when`(Criteria.where("$translations.$lang").ne(null))
        .then("$translations.$lang")
        .otherwise(
            ConditionalOperators.`when`(Criteria.where("$translations.$fallbackLanguage").ne(null))
                .then("$translations.en")
                .otherwise(
                    mapOf("\$getField" to mapOf(
                        "field" to "\$$primaryLanguage",
                        "input" to "\$$translations"
                    ))
                )
        )

    return translationProjection
}

fun moveValuesToRoot(prefix: String) = AggregationOperation {
    Document(
        "\$replaceRoot", Document(
            "newRoot", Document(
                "\$mergeObjects", listOf(
                    "\$$prefix",
                    "\$\$ROOT"
                )
            )
        )
    )
}

fun moveValuesInto(valuePrefix: String, root: String) = AggregationOperation {
    Document(
        "\$set", Document(
            root, Document(
                "\$mergeObjects", listOf(
                    "\$$root",
                    "\$$valuePrefix"
                )
            )
        )
    )
}

fun addField(target: String, doc: Document) = AggregationOperation {
    Document("\$addFields", Document(target, doc))
}
