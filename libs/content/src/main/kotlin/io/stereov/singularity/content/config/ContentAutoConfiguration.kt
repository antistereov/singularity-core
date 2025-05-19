package io.stereov.singularity.content.config

import io.stereov.singularity.content.article.controller.ArticleController
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.common.tag.controller.TagController
import io.stereov.singularity.content.common.tag.repository.TagRepository
import io.stereov.singularity.content.common.tag.service.TagService
import io.stereov.singularity.content.common.util.AccessCriteria
import io.stereov.singularity.content.properties.ContentProperties
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.config.AuthenticationConfiguration
import io.stereov.singularity.core.user.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
@EnableConfigurationProperties(ContentProperties::class)
@EnableReactiveMongoRepositories(basePackageClasses = [ArticleRepository::class, TagRepository::class])
class ContentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun accessCriteria(authenticationService: AuthenticationService): AccessCriteria {
        return AccessCriteria(authenticationService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun articleController(articleService: ArticleService): ArticleController {
        return ArticleController(articleService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun articleService(
        articleRepository: ArticleRepository,
        userService: UserService,
        authenticationService: AuthenticationService,
        tagService: TagService,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        accessCriteria: AccessCriteria,
    ): ArticleService {
        return ArticleService(articleRepository, userService, authenticationService, tagService, reactiveMongoTemplate, accessCriteria)
    }

    @Bean
    @ConditionalOnMissingBean
    fun articleManagementService(articleService: ArticleService, authenticationService: AuthenticationService): ArticleManagementService {
        return ArticleManagementService(articleService, authenticationService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun tagController(service: TagService): TagController {
        return TagController(service)
    }

    @Bean
    @ConditionalOnMissingBean
    fun tagService(repository: TagRepository, reactiveMongoTemplate: ReactiveMongoTemplate, contentProperties: ContentProperties): TagService {
        return TagService(repository, reactiveMongoTemplate, contentProperties)
    }
}
