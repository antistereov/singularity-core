package io.stereov.singularity.user.core.config

import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.config.EmailConfiguration
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.user.core.controller.UserController
import io.stereov.singularity.user.core.exception.handler.UserExceptionHandler
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.repository.UserRepository
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
        ApplicationConfiguration::class,
        AuthenticationConfiguration::class,
        EmailConfiguration:: class,
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [UserRepository::class])
class UserConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userController(
        userService: UserService,
        authorizationService: AuthorizationService,
        userMapper: UserMapper,
        fileStorage: FileStorage,
    ) = UserController(
        userService,
        authorizationService,
        userMapper, fileStorage
    )

    // Mapper

    @Bean
    @ConditionalOnMissingBean
    fun userMapper(fileStorage: FileStorage) = UserMapper(fileStorage)

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun userService(
        userRepository: UserRepository,
        encryptionTransformer: EncryptionService,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        hashService: HashService,
        encryptionSecretService: EncryptionSecretService,
    ): UserService {
        return UserService(
            userRepository,
            encryptionTransformer,
            reactiveMongoTemplate,
            hashService,
            encryptionSecretService,
        )
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun userExceptionHandler() = UserExceptionHandler()
}
