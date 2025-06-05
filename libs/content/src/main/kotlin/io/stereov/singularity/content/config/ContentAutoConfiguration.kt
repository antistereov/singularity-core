package io.stereov.singularity.content.config

import io.stereov.singularity.content.article.controller.ArticleController
import io.stereov.singularity.content.article.controller.ArticleManagementController
import io.stereov.singularity.content.article.queries.ArticleQueries
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.common.content.exception.handler.ContentExceptionHandler
import io.stereov.singularity.content.common.content.util.AccessCriteria
import io.stereov.singularity.content.common.tag.controller.TagController
import io.stereov.singularity.content.common.tag.repository.TagRepository
import io.stereov.singularity.content.common.tag.service.TagService
import io.stereov.singularity.content.properties.ContentProperties
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.auth.config.AuthenticationConfiguration
import io.stereov.singularity.mail.config.MailConfiguration
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.invitation.service.InvitationService
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.user.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
        AuthenticationConfiguration::class,
        MailConfiguration::class
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
    fun articleManagementController(articleManagementService: ArticleManagementService): ArticleManagementController {
        return ArticleManagementController(articleManagementService)
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
        return ArticleService(
            articleRepository,
            userService,
            authenticationService,
            tagService,
            reactiveMongoTemplate,
            accessCriteria,
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun articleManagementService(articleService: ArticleService, authenticationService: AuthenticationService, invitationService: InvitationService, fileStorage: io.stereov.singularity.file.service.FileStorage, translateService: TranslateService, uiProperties: UiProperties, userService: UserService): ArticleManagementService {
        return ArticleManagementService(articleService, authenticationService, invitationService, fileStorage, translateService, uiProperties, userService)
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

    @Bean
    @ConditionalOnMissingBean
    fun articleQueries(reactiveMongoTemplate: ReactiveMongoTemplate, converter: MappingMongoConverter, accessCriteria: AccessCriteria): ArticleQueries {
        return ArticleQueries(reactiveMongoTemplate, converter, accessCriteria)
    }

    @Bean
    @ConditionalOnMissingBean
    fun contentExceptionHandler() = ContentExceptionHandler()
}
