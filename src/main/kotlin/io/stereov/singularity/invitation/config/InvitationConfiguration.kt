package io.stereov.singularity.invitation.config

import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.invitation.controller.InvitationController
import io.stereov.singularity.invitation.exception.handler.InvitationExceptionHandler
import io.stereov.singularity.invitation.repository.InvitationRepository
import io.stereov.singularity.invitation.service.InvitationService
import io.stereov.singularity.invitation.service.InvitationTokenService
import io.stereov.singularity.jwt.service.JwtService
import io.stereov.singularity.mail.config.MailConfiguration
import io.stereov.singularity.mail.service.MailService
import io.stereov.singularity.template.service.TemplateService
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.user.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        MailConfiguration::class
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
        mailService: MailService,
        translateService: TranslateService,
        userService: UserService,
        uiProperties: UiProperties,
    ): InvitationService {
        return InvitationService(repository, encryptionService, encryptionSecretService, reactiveMongoTemplate, invitationTokenService, templateService, mailService, translateService, userService, uiProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun invitationTokenService(jwtService: JwtService) = InvitationTokenService(jwtService)

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun invitationController(invitationService: InvitationService) = InvitationController(invitationService)

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun invitationExceptionHandler() = InvitationExceptionHandler()
}
