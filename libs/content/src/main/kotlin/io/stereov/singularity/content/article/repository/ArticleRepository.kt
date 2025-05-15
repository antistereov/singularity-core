package io.stereov.singularity.content.article.repository

import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.common.repository.ContentRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : ContentRepository<Article> {

    suspend fun findByIdLessThanOrderByIdDesc(id: String, limit: Long): Flow<Article>
}
