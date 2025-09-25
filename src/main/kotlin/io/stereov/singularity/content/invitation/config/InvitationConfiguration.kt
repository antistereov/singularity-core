package io.stereov.singularity.content.invitation.config

import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.content.invitation.controller.InvitationController
import io.stereov.singularity.content.invitation.exception.handler.InvitationExceptionHandler
import io.stereov.singularity.content.invitation.repository.InvitationRepository
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.content.invitation.service.InvitationTokenService
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.email.core.config.EmailConfiguration
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        EmailConfiguration::class
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [InvitationRepository::class])
class InvitationConfiguration {

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun invitationService(
        repository: InvitationRepository,
        encryptionService: EncryptionService,
        encryptionSecretService: EncryptionSecretService,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        invitationTokenService: InvitationTokenService,
        templateService: TemplateService,
        emailService: EmailService,
        translateService: TranslateService,
        userService: UserService,
        uiProperties: UiProperties,
        appProperties: AppProperties
    ): InvitationService {
        return InvitationService(
            repository,
            encryptionService,
            encryptionSecretService,
            reactiveMongoTemplate,
            invitationTokenService,
            templateService,
            emailService,
            translateService,
            userService,
            uiProperties,
            appProperties
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun invitationTokenService(jwtService: JwtService) = InvitationTokenService(jwtService)

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun invitationController(
        invitationService: InvitationService,
        context: ApplicationContext
    ) = InvitationController(invitationService, context)

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun invitationExceptionHandler() = InvitationExceptionHandler()
}
