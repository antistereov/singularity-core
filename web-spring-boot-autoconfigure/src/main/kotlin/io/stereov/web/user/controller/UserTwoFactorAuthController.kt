package io.stereov.web.user.controller

import io.stereov.web.config.Constants
import io.stereov.web.user.dto.TwoFactorSetupResponseDto
import io.stereov.web.user.dto.TwoFactorStatusResponse
import io.stereov.web.user.dto.UserDto
import io.stereov.web.user.service.CookieService
import io.stereov.web.user.service.UserTwoFactorAuthService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ServerWebExchange

@Controller
@RequestMapping("/user/2fa")
class UserTwoFactorAuthController(
    private val twoFactorService: UserTwoFactorAuthService,
    private val cookieService: CookieService,
) {

    @PostMapping("/setup")
    suspend fun setupTwoFactorAuth(): ResponseEntity<TwoFactorSetupResponseDto> {
        val res = twoFactorService.setUpTwoFactorAuth()

        return ResponseEntity.ok().body(res)
    }

    @PostMapping("/verify")
    suspend fun verifyTwoFactorAuth(@RequestParam("code") code: Int, exchange: ServerWebExchange): ResponseEntity<UserDto> {
        val userDto = twoFactorService.validateTwoFactorCode(exchange, code)

        val clearTwoFactorCookie = cookieService.clearTwoFactorSessionCookie()
        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .body(userDto)
    }

    @GetMapping("/status")
    suspend fun getTwoFactorAuthStatus(exchange: ServerWebExchange): ResponseEntity<TwoFactorStatusResponse> {
        val isPending = !exchange.request.cookies[Constants.TWO_FACTOR_AUTH_COOKIE]?.firstOrNull()?.value.isNullOrBlank()
        return ResponseEntity.ok().body(
            TwoFactorStatusResponse(isPending)
        )
    }

    @PostMapping("/recovery")
    suspend fun recoverUser(@RequestParam("code") code: String, exchange: ServerWebExchange): ResponseEntity<UserDto> {
        val userDto = twoFactorService.recoverUser(exchange, code)

        val clearTwoFactorCookie = cookieService.clearTwoFactorSessionCookie()
        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .body(userDto)
    }
}
