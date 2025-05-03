package io.stereov.singularity.content.config

import io.stereov.singularity.content.article.controller.ArticleController
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.article.service.UserArticleService
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.config.AuthenticationConfiguration
import io.stereov.singularity.core.user.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
        AuthenticationConfiguration::class,
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [ArticleRepository::class])
class ContentAutoConfiguration {


    @Bean
    @ConditionalOnMissingBean
    fun articleController(articleService: ArticleService, userArticleService: UserArticleService): ArticleController {
        return ArticleController(articleService, userArticleService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun articleService(articleRepository: ArticleRepository, reactiveMongoTemplate: ReactiveMongoTemplate, userService: UserService, authenticationService: AuthenticationService): ArticleService {
        return ArticleService(articleRepository, reactiveMongoTemplate, userService, authenticationService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userArticleService(authenticationService: AuthenticationService, reactiveMongoTemplate: ReactiveMongoTemplate): UserArticleService {
        return UserArticleService(reactiveMongoTemplate, authenticationService)
    }


}
