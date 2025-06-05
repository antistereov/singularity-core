package io.stereov.singularity.content.article.queries

import io.stereov.singularity.content.article.dto.ArticleOverviewResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.common.content.util.AccessCriteria
import io.stereov.singularity.database.model.PagedResult
import io.stereov.singularity.database.util.pageFromPagedResult
import io.stereov.singularity.translate.model.Language
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.BsonNull
import org.bson.Document
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.*
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators.IfNull.ifNull
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component


@Component
class ArticleQueries(
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val converter: MappingMongoConverter,
    private val accessCriteria: AccessCriteria,
) {

    suspend fun getArticles(pageable: Pageable, tags: List<String>, lang: Language): Page<ArticleOverviewResponse> {
        val baseCriteria = accessCriteria.getViewCriteria()

        val combinedCriteria = if (tags.isEmpty()) {
            baseCriteria
        } else {
            Criteria().andOperator(
                baseCriteria,
                Criteria.where(Article::tags.name).`in`(tags)
            )
        }

        val skip = Aggregation.skip(pageable.pageSize * pageable.pageNumber.toLong())
        val limit = Aggregation.limit(pageable.pageSize.toLong())
        val unwindTags = Aggregation.unwind("tags", true)
        val lookupTags = LookupOperation.newLookup()
            .from("tags")
            .localField("tags")
            .foreignField("key")
            .`as`("tag")
        val translatedTagField = AddFieldsOperation("translatedTag",
            Document("\$cond", listOf(
                // Condition: $ne([$ifNull:["$tag", null], null])
                Document("\$ne", listOf(
                    Document("\$ifNull", listOf("\$tag", BsonNull())),
                    BsonNull()
                )),
                // then
                Document("\$mergeObjects", listOf(
                    Document("key", "\$tag.key"),
                    Document("\$cond", listOf(
                        Document("\$ne", listOf(
                            Document("\$ifNull", listOf(
                                Document("\$getField", Document()
                                    .append("field", Document("\$literal", "\$CURRENT.translationLang"))
                                    .append("input", "\$tag.translations")),
                                BsonNull()
                            )),
                            BsonNull()
                        )),
                        Document("\$getField", Document()
                            .append("field", Document("\$literal", "\$CURRENT.translationLang"))
                            .append("input", "\$tag.translations")),
                        Document("\$cond", listOf(
                            Document("\$ne", listOf(
                                Document("\$ifNull", listOf("\$tag.translations.en", BsonNull())),
                                BsonNull()
                            )),
                            "\$tag.translations.en",
                            Document("\$getField", Document()
                                .append("field", Document("\$literal", "\$CURRENT.tag.primaryLanguage"))
                                .append("input", "\$tag.translations"))
                        ))
                    ))
                )),
                // else
                "\$\$REMOVE"
            ))
        )
        val translateTag = Aggregation.addFields()
            .addField("translatedTag")
            .withValue(translatedTagField).build()
            val group = Aggregation.group("_id")
                .push("translatedTag").`as`("tags")
                .first("key").`as`("key")
                .first("createdAt").`as`("createdAt")
                .first("publishedAt").`as`("publishedAt")
                .first("updatedAt").`as`("updatedAt")
                .first("path").`as`("path")
                .first("state").`as`("state")
                .first("colors").`as`("colors")
                .first("image").`as`("image")
                .first("translations").`as`("translations")
            val project = Aggregation.project()
                .and("_id").`as`("id")
                .andInclude(
                    "key",
                    "createdAt",
                    "publishedAt",
                    "updatedAt",
                    "path",
                    "state",
                    "colors",
                    "image",
                    "tags"
                )
                .and(
                    ConditionalOperators
                        .`when`(
                            ComparisonOperators.Ne.valueOf(
                                ifNull("translations.$lang").then("ne")
                            ).notEqualToValue("ne")
                        )
                        .then(lang)
                        .otherwise(
                            ConditionalOperators
                                .`when`(
                                    ComparisonOperators.Ne.valueOf(
                                        ifNull("translations.en").then("ne")
                                    ).notEqualToValue("ne")
                                )
                                .then("en")
                                .otherwise("\$primaryLanguage")
                        )
                ).`as`("lang")
                .and(
                    ConditionalOperators
                        .`when`(
                            ComparisonOperators.Ne.valueOf(
                                ifNull("translations.$lang").then("ne")
                            ).notEqualToValue("ne")
                        )
                        .thenValueOf("\$translations.$lang")
                        .otherwise(
                            ConditionalOperators
                                .`when`(
                                    ComparisonOperators.Ne.valueOf(
                                        ifNull("translations.en").then("ne")
                                    ).notEqualToValue("ne")
                                )
                                .thenValueOf("\$translations.en")
                                .otherwise(
                                    ObjectOperators.GetField.getField("translations")
                                        .of("\$primaryLanguage")
                                )
                        )
                ).`as`("translation")
            val replaceRoot = Aggregation.replaceRoot()
                .withValueOf(
                    ObjectOperators.MergeObjects.merge(
                        "\$translation",
                        "\$\$ROOT"
                    )
                )
            val finalize = Aggregation.project()
                .and("id").`as`("id")
                .andInclude(
                    "key",
                    "createdAt",
                    "publishedAt",
                    "updatedAt",
                    "path",
                    "state",
                    "colors",
                    "image",
                    "title",
                    "summary",
                    "tags",
                    "lang"
                )


            val aggregation = TypedAggregation<PagedResult>.newAggregation(
                PagedResult::class.java,
                Aggregation.match(combinedCriteria),

                Aggregation.facet(
                    Aggregation.count().`as`("total")
                ).`as`("count")
                    .and(
                        skip,
                        limit,
                        unwindTags,
                        lookupTags,
                        translateTag,
                        group,
                        project,
                        replaceRoot,
                        finalize
                    ).`as`("content")
            )

            val result = reactiveMongoTemplate.aggregate(aggregation, PagedResult::class.java)
                .collectList().awaitFirstOrNull()
                ?: emptyList()

            return pageFromPagedResult<ArticleOverviewResponse>(result, pageable, converter)
        }
}
