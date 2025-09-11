package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.twofactor.service.MailAuthenticationService
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.global.model.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/2fa/mail")
class MailAuthenticationController(
    private val mailAuthenticationService: MailAuthenticationService,
    private val authorizationService: AuthorizationService
) {

    @PostMapping("/send")
    suspend fun sendAuthenticationMail(
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<SuccessResponse> {
        val user = authorizationService.getCurrentUser()

        mailAuthenticationService.sendMail(user, lang)

        return ResponseEntity.ok(SuccessResponse())
    }
}