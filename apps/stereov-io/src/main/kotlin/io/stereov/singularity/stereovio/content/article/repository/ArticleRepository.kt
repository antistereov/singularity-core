package io.stereov.singularity.stereovio.content.article.repository

import io.stereov.singularity.stereovio.content.article.model.Article
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : CoroutineCrudRepository<Article, String> {

    suspend fun findByKey(key: String): Article?
}
