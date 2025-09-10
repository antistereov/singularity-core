package io.stereov.singularity.content.article.config

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.article.controller.ArticleController
import io.stereov.singularity.content.article.controller.ArticleManagementController
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.config.ContentConfiguration
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@AutoConfiguration(
    after = [
        ContentConfiguration::class
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [ArticleRepository::class])
class ArticleConfiguration {

    // Controller

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

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun articleService(
        articleRepository: ArticleRepository,
        userService: UserService,
        authorizationService: AuthorizationService,
        tagService: TagService,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        accessCriteria: AccessCriteria,
        fileStorage: FileStorage,
        userMapper: UserMapper,
    ): ArticleService {
        return ArticleService(
            articleRepository,
            userService,
            authorizationService,
            tagService,
            reactiveMongoTemplate,
            accessCriteria,
            fileStorage,
            userMapper
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun articleManagementService(
        articleService: ArticleService,
        authorizationService: AuthorizationService,
        invitationService: InvitationService,
        fileStorage: FileStorage,
        translateService: TranslateService,
        uiProperties: UiProperties,
        userService: UserService,
        userMapper: UserMapper
    ): ArticleManagementService {
        return ArticleManagementService(
            articleService,
            authorizationService,
            invitationService,
            fileStorage,
            translateService,
            uiProperties,
            userService,
            userMapper
        )
    }
}
