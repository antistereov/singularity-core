package io.stereov.singularity.auth.guest.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.model.IdentityProvider
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.auth.guest.dto.request.ConvertToUserRequest
import io.stereov.singularity.auth.guest.dto.request.CreateGuestRequest
import io.stereov.singularity.auth.guest.exception.model.AccountIsAlreadyUserException
import io.stereov.singularity.auth.guest.mapper.GuestMapper
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.user.core.exception.model.EmailAlreadyTakenException
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.AccountDocument
import io.stereov.singularity.user.core.model.UserSecurityDetails
import io.stereov.singularity.user.core.model.identity.UserIdentity
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.util.*

@Service
class GuestService(
    private val userService: UserService,
    private val twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
    private val authorizationService: AuthorizationService,
    private val emailProperties: EmailProperties,
    private val hashService: HashService,
    private val emailVerificationService: EmailVerificationService,
    private val accessTokenCache: AccessTokenCache,
    private val guestMapper: GuestMapper
) {

    private val logger = KotlinLogging.logger {}

    suspend fun createGuest(req: CreateGuestRequest): AccountDocument.Guest {
        logger.debug { "Creating guest with name ${req.name}" }

        val userDocument = guestMapper.createGuest(
            name = req.name,
            mailTwoFactorCodeExpiresIn = twoFactorEmailCodeProperties.expiresIn
        )

        return userService.save(userDocument)
    }

    suspend fun convertToUser(
        accountId: ObjectId,
        req: ConvertToUserRequest,
        locale: Locale?
    ): AccountDocument.Guest {
        val user = userService.findById(accountId)

        if (user is AccountDocument.User)
            throw AccountIsAlreadyUserException("Cannot convert account to user: account is already user")

        if (userService.existsByEmail(req.email))
            throw EmailAlreadyTakenException("Cannot convert account to user: email is already taken")

        val emailEnabled = emailProperties.enable

        user.sensitive.email = req.email
        user.sensitive.identities[IdentityProvider.PASSWORD] = UserIdentity(
            hashService.hashBcrypt(req.password),
            null
        )
        user.sensitive.security = UserSecurityDetails(
            emailEnabled,
            twoFactorEmailCodeProperties.expiresIn
        )
        user.roles.clear()
        user.roles.add(Role.USER)

        val savedUserDocument = userService.save(user)
        accessTokenCache.invalidateAllTokens(userId)

        if (emailEnabled) emailVerificationService.sendVerificationEmail(savedUserDocument, locale)

        return savedUserDocument
    }
}