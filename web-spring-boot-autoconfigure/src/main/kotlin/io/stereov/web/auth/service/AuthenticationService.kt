package io.stereov.web.auth.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.auth.exception.InvalidPrincipalException
import io.stereov.web.auth.model.CustomAuthenticationToken
import io.stereov.web.global.service.jwt.exception.InvalidTokenException
import io.stereov.web.user.model.UserDocument
import io.stereov.web.user.service.UserService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service

@Service
class AuthenticationService(
    private val userService: UserService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun getCurrentUserId(): String {
        logger.debug {"Extracting user ID." }

        val auth = getCurrentAuthentication()
        return auth.userId
    }

    suspend fun getCurrentUser(): UserDocument {
        logger.debug { "Extracting current user" }

        val userId = getCurrentUserId()
        return userService.findById(userId)
    }

    suspend fun getCurrentDeviceId(): String {
        logger.debug { "Extracting device ID" }

        val auth = getCurrentAuthentication()
        return auth.deviceId
    }

    private suspend fun getCurrentAuthentication(): CustomAuthenticationToken {
        val securityContext: SecurityContext = ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()
            ?: throw InvalidPrincipalException("No security context found.")

        val authentication = securityContext.authentication
            ?: throw InvalidTokenException("Authentication is missing.")

        return authentication as? CustomAuthenticationToken
                ?: throw InvalidTokenException("Authentication does not contain needed properties")
    }
}
