package io.stereov.web.auth.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.jwt.exception.InvalidTokenException
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service

@Service
class AuthenticationService {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun getCurrentAuth(): io.stereov.web.auth.model.CustomAuthenticationToken {
        logger.debug { "Extracting AuthInfo" }

        val auth = getCurrentAuthentication()
        return auth
    }

    suspend fun getCurrentAccountId(): String {
        logger.debug {"Extracting user ID." }

        val auth = getCurrentAuthentication()
        return auth.accountId
    }

    private suspend fun getCurrentAuthentication(): io.stereov.web.auth.model.CustomAuthenticationToken {
        val securityContext: SecurityContext = ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()
            ?: throw io.stereov.web.auth.exception.InvalidPrincipalException("No security context found.")

        val authentication = securityContext.authentication
            ?: throw InvalidTokenException("Authentication is missing.")

        return authentication as? io.stereov.web.auth.model.CustomAuthenticationToken
                ?: throw InvalidTokenException("Authentication does not contain needed properties")
    }
}
