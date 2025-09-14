package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorAuthenticationRequest
import io.stereov.singularity.auth.twofactor.exception.model.InvalidTwoFactorRequestException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange

@Service
class TwoFactorAuthenticationService(
    private val userService: UserService,
    private val totpAuthenticationService: TotpAuthenticationService,
    private val twoFactorAuthTokenService: TwoFactorAuthenticationTokenService,
    private val mailAuthenticationService: MailAuthenticationService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun handleTwoFactor(user: UserDocument, lang: Language) {

        if (user.sensitive.security.twoFactor.preferred == TwoFactorMethod.MAIL) {
            mailAuthenticationService.sendMail(user, lang)
        }
    }

    /**
     * Validates the two-factor code for the current user.
     *
     * @param exchange The server web exchange containing the request and response.
     * @param code The two-factor code to validate.
     *
     * @throws io.stereov.singularity.global.exception.model.InvalidDocumentException If the user document does not contain a two-factor authentication secret.
     * @throws AuthException If the two-factor code is invalid.
     */
    suspend fun validateTwoFactor(exchange: ServerWebExchange, req: TwoFactorAuthenticationRequest): UserDocument {
        logger.debug { "Validating two factor code" }

        val token = twoFactorAuthTokenService.extract(exchange)
        val user = userService.findById(token.userId)

        if (user.sensitive.security.twoFactor.totp.enabled) {
            req.totp?.let { return totpAuthenticationService.validateCode(user, it) }
        }
        if (user.sensitive.security.twoFactor.mail.enabled) {
            req.mail?.let { return mailAuthenticationService.validateCode(user, it) }
        }

        throw InvalidTwoFactorRequestException("2FA failed: no valid code found in request, available methods: ${user.twoFactorMethods}")
    }

}
