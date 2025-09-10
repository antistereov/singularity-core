package io.stereov.singularity.global.config

import io.stereov.singularity.auth.core.model.SessionTokenType
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.global.model.OpenApiConstants
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import org.springframework.boot.autoconfigure.AutoConfiguration

@AutoConfiguration
@SecurityScheme(
    name = OpenApiConstants.ACCESS_TOKEN_HEADER,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Access token for user authentication."
)
@SecurityScheme(
    name = OpenApiConstants.ACCESS_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = TwoFactorTokenType.InitSetup.COOKIE_NAME,
    description = "Access token for user authentication."
)

@SecurityScheme(
    name = OpenApiConstants.REFRESH_TOKEN_HEADER,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    paramName = SessionTokenType.Access.HEADER,
    description = "Refresh token to request new access tokens"
)
@SecurityScheme(
    name = OpenApiConstants.REFRESH_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = SessionTokenType.Refresh.COOKIE_NAME,
    description = "Refresh token to request new access tokens via Cookie"
)

@SecurityScheme(
    name = OpenApiConstants.STEP_UP_HEADER,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = TwoFactorTokenType.StepUp.HEADER,
    description = "Token for step up authentication allowing access of secure resources."
)
@SecurityScheme(
    name = OpenApiConstants.STEP_UP_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = TwoFactorTokenType.StepUp.COOKIE_NAME,
    description = "Token for step up authentication allowing access of secure resources."
)

@SecurityScheme(
    name = OpenApiConstants.TWO_FACTOR_INIT_SETUP_HEADER,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = TwoFactorTokenType.InitSetup.HEADER,
    description = "Token that allows 2FA setup"
)
@SecurityScheme(
    name = OpenApiConstants.TWO_FACTOR_INIT_SETUP_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = TwoFactorTokenType.InitSetup.COOKIE_NAME,
    description = "Token that allows 2FA setup via Cookie"
)

@SecurityScheme(
    name = OpenApiConstants.TWO_FACTOR_LOGIN_HEADER,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = TwoFactorTokenType.Login.HEADER,
    description = "Token for successful authentication with email and password, indicating 2FA is required."
)
@SecurityScheme(
    name = OpenApiConstants.TWO_FACTOR_LOGIN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = TwoFactorTokenType.Login.COOKIE_NAME,
    description = "Token for successful authentication with email and password, indicating 2FA is required."
)

class OpenApiConfig
