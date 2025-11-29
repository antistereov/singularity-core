package io.stereov.singularity.content.article.config

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.principal.group.service.GroupService
import io.stereov.singularity.content.article.controller.ArticleController
import io.stereov.singularity.content.article.controller.ArticleManagementController
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.properties.ArticleProperties
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.article.service.ArticleManagementService
import io.stereov.singularity.content.article.service.ArticleService
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.config.ContentConfiguration
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.invitation.mapper.InvitationMapper
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
@ConditionalOnProperty(prefix = "singularity.content.articles", value = ["enable"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ArticleProperties::class)
class ArticleConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun articleController(
        articleService: ArticleService,
        authorizationService: AuthorizationService,
    ) = ArticleController(
        articleService,
        authorizationService
    )


    @Bean
    @ConditionalOnMissingBean
    fun articleManagementController(
        articleManagementService: ArticleManagementService,
        authorizationService: AuthorizationService,
    ): ArticleManagementController {
        return ArticleManagementController(
            articleManagementService,
            authorizationService,
        )
    }

    // Mapper

    @Bean
    @ConditionalOnMissingBean
    fun articleMapper(
        appProperties: AppProperties,
        userService: UserService,
        translateService: TranslateService,
        tagMapper: TagMapper,
        tagService: TagService,
        fileStorage: FileStorage,
        principalMapper: PrincipalMapper,
    ) = ArticleMapper(
        appProperties,
        userService,
        translateService,
        tagMapper,
        tagService,
        fileStorage,
        principalMapper,
    )

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun articleService(
        articleRepository: ArticleRepository,
        authorizationService: AuthorizationService,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        accessCriteria: AccessCriteria,
        articleMapper: ArticleMapper,
        translateService: TranslateService,
        contentProperties: ContentProperties
    ): ArticleService {
        return ArticleService(
            articleRepository,
            authorizationService,
            reactiveMongoTemplate,
            accessCriteria,
            translateService,
            articleMapper,
            contentProperties
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
        userService: UserService,
        principalMapper: PrincipalMapper,
        articleMapper: ArticleMapper,
        imageStore: ImageStore,
        groupService: GroupService,
        invitationMapper: InvitationMapper
    ): ArticleManagementService {
        return ArticleManagementService(
            articleService,
            authorizationService,
            invitationService,
            translateService,
            userService,
            principalMapper,
            fileStorage,
            articleMapper,
            imageStore,
            groupService,
            invitationMapper,
        )
    }
}
