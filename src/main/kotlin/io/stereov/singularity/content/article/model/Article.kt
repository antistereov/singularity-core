package io.stereov.singularity.content.article.model

import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.translate.model.Translatable
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.*

@Document(collection = "articles")
data class Article(
    @Id override val _id: ObjectId? = null,
    @Indexed(unique = true) override var key: String,
    override var createdAt: Instant = Instant.now(),
    override var updatedAt: Instant = Instant.now(),
    override var access: ContentAccessDetails,
    var publishedAt: Instant? = null,
    var path: String,
    var state: ArticleState = ArticleState.DRAFT,
    var colors: ArticleColors = ArticleColors(),
    var imageKey: String? = null,
    override var trusted: Boolean,
    override var tags: MutableSet<String> = mutableSetOf(),
    override val translations: MutableMap<Locale, ArticleTranslation> = mutableMapOf(),
) : ContentDocument<Article>, Translatable<ArticleTranslation> {

    companion object {
        const val CONTENT_TYPE = "articles"
    }
}
