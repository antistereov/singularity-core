package io.stereov.singularity.admin.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.admin.core.exception.model.AtLeastOneAdminRequiredException
import io.stereov.singularity.admin.core.exception.model.GuestCannotBeAdminException
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.AccountDocument
import io.stereov.singularity.user.core.service.UserService
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val userService: UserService,
    private val appProperties: AppProperties,
    private val hashService: HashService,
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val emailProperties: EmailProperties,
    private val authorizationService: AuthorizationService,
    private val userMapper: UserMapper,
    private val accessTokenCache: AccessTokenCache
) {

    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun init() {
        runBlocking { initRootAccount() }
    }

    suspend fun addAdminRole(userId: ObjectId): UserResponse {
        logger.debug { "Adding admin role to user $userId" }

        authorizationService.requireRole(Role.ADMIN)

        val user = userService.findById(userId)

        if (user.isGuest)
            throw GuestCannotBeAdminException("A guest cannot be admin")

        user.addRole(Role.ADMIN)
        val updatedUser = userService.save(user)

        accessTokenCache.invalidateAllTokens(user.id)

        return userMapper.toResponse(updatedUser)
    }

    suspend fun revokeAdminRole(userId: ObjectId): UserResponse {
        logger.debug { "Revoking admin role from user $userId" }

        authorizationService.requireRole(Role.ADMIN)

        if (userService.findAllByRolesContaining(Role.ADMIN).count() == 1)
            throw AtLeastOneAdminRequiredException("Cannot revoke admin role for last existing admin: at least one admin is required")

        val user = userService.findById(userId)
        user.roles.remove(Role.ADMIN)
        val updatedUser = userService.save(user)

        accessTokenCache.invalidateAllTokens(user.id)

        return userMapper.toResponse(updatedUser)
    }

    private suspend fun initRootAccount() {
        if (!this.userService.existsByEmail(appProperties.rootEmail) && appProperties.createRootUser) {
            this.logger.info { "Creating root account" }

            val rootUser = AccountDocument.ofPassword(
                email = appProperties.rootEmail,
                password = hashService.hashBcrypt(appProperties.rootPassword),
                name = "Root",
                email2faEnabled = emailProperties.enable,
                mailTwoFactorCodeExpiresIn = twoFactorEmailCodeProperties.expiresIn
            ).addRole(Role.ADMIN)

            this.userService.save(rootUser)
        } else {
            this.logger.info { "Root account exists, skipping creation" }
        }
    }
}
