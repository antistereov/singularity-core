package io.stereov.singularity.user.core.config

import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.RefreshTokenService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.config.EmailConfiguration
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.user.core.controller.GuestController
import io.stereov.singularity.user.core.controller.UserController
import io.stereov.singularity.user.core.mapper.GuestMapper
import io.stereov.singularity.user.core.mapper.PrincipalMapper
import io.stereov.singularity.user.core.repository.GuestRepository
import io.stereov.singularity.user.core.repository.UserRepository
import io.stereov.singularity.user.core.service.ConvertGuestToUserService
import io.stereov.singularity.user.core.service.GuestService
import io.stereov.singularity.user.core.service.PrincipalService
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
        EmailConfiguration::class,
    ]
)
@EnableReactiveMongoRepositories(
    basePackageClasses = [
        GuestRepository::class,
        UserRepository::class,
    ]
)
class UserConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun guestController(
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        principalMapper: PrincipalMapper,
        guestService: GuestService,
        authProperties: AuthProperties,
        geolocationService: GeolocationService,
        cookieCreator: CookieCreator,
        authorizationService: AuthorizationService,
        convertGuestToUserService: ConvertGuestToUserService,
    ) = GuestController(
        accessTokenService,
        refreshTokenService,
        principalMapper,
        guestService,
        authProperties,
        geolocationService,
        cookieCreator,
        authorizationService,
        convertGuestToUserService
    )

    @Bean
    @ConditionalOnMissingBean
    fun userController(
        userService: UserService,
        authorizationService: AuthorizationService,
        principalMapper: PrincipalMapper,
        fileStorage: FileStorage,
    ) = UserController(
        userService,
        authorizationService,
        principalMapper, fileStorage
    )

    // Mapper

    @Bean
    @ConditionalOnMissingBean
    fun guestMapper() = GuestMapper()

    @Bean
    @ConditionalOnMissingBean
    fun principalMapper(fileStorage: FileStorage) = PrincipalMapper(fileStorage)

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun convertGuestToUserService(
        principalService: PrincipalService,
        userService: UserService,
        emailProperties: EmailProperties,
        twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
        accessTokenCache: AccessTokenCache,
        emailVerificationService: EmailVerificationService,
        hashService: HashService,
    ) = ConvertGuestToUserService(
        principalService,
        userService,
        emailProperties,
        twoFactorEmailCodeProperties,
        accessTokenCache,
        emailVerificationService,
        hashService
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun guestService(
        repository: GuestRepository,
        encryptionService: EncryptionService,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        encryptionSecretService: EncryptionSecretService,
        guestMapper: GuestMapper,
    ) = GuestService(
        repository,
        encryptionService,
        reactiveMongoTemplate,
        encryptionSecretService,
        guestMapper
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun principalService(
        reactiveMongoTemplate: ReactiveMongoTemplate,
        userService: UserService,
        guestService: GuestService
    ) = PrincipalService(
        reactiveMongoTemplate,
        userService,
        guestService
    )

    @Bean
    @ConditionalOnMissingBean
    fun userService(
        userRepository: UserRepository,
        encryptionService: EncryptionService,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        hashService: HashService,
        encryptionSecretService: EncryptionSecretService,
    ): UserService {
        return UserService(
            userRepository,
            encryptionService,
            reactiveMongoTemplate,
            hashService,
            encryptionSecretService,
        )
    }
}
