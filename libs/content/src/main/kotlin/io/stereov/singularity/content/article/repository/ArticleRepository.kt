package io.stereov.singularity.content.article.repository

import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.common.repository.ContentRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : ContentRepository<Article>
