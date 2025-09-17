package io.stereov.singularity.global.config

import io.stereov.singularity.auth.core.controller.AuthenticationController
import io.stereov.singularity.auth.core.controller.EmailVerificationController
import io.stereov.singularity.auth.core.controller.PasswordResetController
import io.stereov.singularity.auth.core.controller.SessionController
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.group.controller.GroupController
import io.stereov.singularity.auth.group.controller.GroupMemberController
import io.stereov.singularity.auth.oauth2.controller.IdentityProviderController
import io.stereov.singularity.auth.oauth2.controller.OAuth2ProviderController
import io.stereov.singularity.auth.twofactor.controller.EmailAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TotpAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TwoFactorAuthenticationController
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.global.model.OpenApiConstants
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean

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
    paramName = SessionTokenType.Access.COOKIE_NAME,
    description = "Access token for user authentication."
)

@SecurityScheme(
    name = OpenApiConstants.REFRESH_TOKEN_HEADER,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Refresh token to request new access tokens."
)
@SecurityScheme(
    name = OpenApiConstants.REFRESH_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = SessionTokenType.Refresh.COOKIE_NAME,
    description = "Refresh token to request new access tokens."
)

@SecurityScheme(
    name = OpenApiConstants.STEP_UP_TOKEN_HEADER,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = SessionTokenType.StepUp.HEADER,
    description = "Token for step up authentication allowing access of secure resources."
)
@SecurityScheme(
    name = OpenApiConstants.STEP_UP_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = SessionTokenType.StepUp.COOKIE_NAME,
    description = "Token for step up authentication allowing access of secure resources."
)

@SecurityScheme(
    name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = TwoFactorTokenType.Authentication.HEADER,
    description = "Token for successful authentication with email and password, indicating 2FA is required."
)
@SecurityScheme(
    name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = TwoFactorTokenType.Authentication.COOKIE_NAME,
    description = "Token for successful authentication with email and password, indicating 2FA is required."
)

class OpenApiConfig() {

    @Bean
    fun customize(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        sortEndpoints(openApi)
        sortTags(openApi)
        sortSecurityRequirements(openApi)
    }

    private fun sortTags(openAPI: OpenAPI) {
        val sortedTagNames = listOf(
            "Authentication",
            "Two-Factor Authentication",
            "OAuth2",
            "Sessions",
            "Groups",
        )

        val sortedTags = mutableListOf<Tag>()

        sortedTagNames.forEach { tagName ->
            openAPI.tags.find { it.name == tagName }?.let { sortedTags.add(it) }
        }

        openAPI.tags.forEach { tag ->
            if (!sortedTagNames.contains(tag.name)) {
                sortedTags.add(tag)
            }
        }

        openAPI.tags = sortedTags
    }

    private fun sortEndpoints(openApi: OpenAPI) {
        val sortedOperationIds = mutableListOf<String>()

        // Authentication
        sortedOperationIds.addAll(listOf(
            AuthenticationController::register.name,
            AuthenticationController::login.name,
            AuthenticationController::logout.name,
            AuthenticationController::refreshAccessToken.name,
            AuthenticationController::stepUp.name,
            AuthenticationController::getAuthenticationStatus.name,

            PasswordResetController::sendPasswordResetEmail.name,
            PasswordResetController::resetPassword.name,
            PasswordResetController::getRemainingPasswordResetCooldown.name,

            EmailVerificationController::sendEmailVerificationEmail.name,
            EmailVerificationController::verifyEmail.name,
            EmailVerificationController::getRemainingEmailVerificationCooldown.name,
        ))

        // Two-Factor Authentication
        sortedOperationIds.addAll(listOf(
            TwoFactorAuthenticationController::completeLogin.name,
            TwoFactorAuthenticationController::completeStepUp.name,

            TwoFactorAuthenticationController::changePreferredMethod.name,

            TotpAuthenticationController::getTotpSetupDetails.name,
            TotpAuthenticationController::enableTotpAsTwoFactorMethod.name,
            TotpAuthenticationController::disableTotpAsTwoFactorMethod.name,
            TotpAuthenticationController::enableTotpAsTwoFactorMethod.name,
            TotpAuthenticationController::recoverFromTotp.name,

            EmailAuthenticationController::sendEmailTwoFactorCode.name,
            EmailAuthenticationController::enableEmailAsTwoFactorMethod.name,
            EmailAuthenticationController::disableEmailAsTwoFactorMethod.name,
            EmailAuthenticationController::getRemainingEmailTwoFactorCooldown.name
        ))

        // OAuth2

        sortedOperationIds.addAll(listOf(
            IdentityProviderController::getIdentityProviders.name,
            IdentityProviderController::deleteIdentityProvider.name,

            IdentityProviderController::addPasswordAuthentication.name,
            OAuth2ProviderController::generateOAuth2ProviderConnectionToken.name
        ))

        // Sessions

        sortedOperationIds.addAll(listOf(
            SessionController::getActiveSessions.name,
            SessionController::deleteSession.name,
            SessionController::deleteAllSessions.name,
            SessionController::generateSessionToken.name
        ))

        // Groups

        sortedOperationIds.addAll(listOf(
            GroupController::createGroup.name,
            GroupController::getGroups.name,
            GroupController::getGroupByKey.name,
            GroupController::updateGroup.name,
            GroupController::deleteGroup.name,

            GroupMemberController::addMemberToGroup.name,
            GroupMemberController::removeMemberFromGroup.name
        ))

        val allOperations = mutableListOf<Triple<String, PathItem.HttpMethod, Operation>>()
        openApi.paths.forEach { (path, pathItem) ->
            pathItem.readOperationsMap().forEach { (method, operation) ->
                allOperations.add(Triple(path, method, operation))
            }
        }

        allOperations.sortWith(compareBy { (_, _, operation) ->
            val operationIdIndex = sortedOperationIds.indexOf(operation.operationId)
            if (operationIdIndex != -1) {
                operationIdIndex
            } else {
                sortedOperationIds.size + 1
            }
        })

        val sortedPaths = LinkedHashMap<String, PathItem>()
        allOperations.forEach { (path, method, operation) ->
            val pathItem = sortedPaths.getOrPut(path) { PathItem() }
            when (method) {
                PathItem.HttpMethod.POST -> pathItem.post = operation
                PathItem.HttpMethod.GET -> pathItem.get = operation
                PathItem.HttpMethod.PUT -> pathItem.put = operation
                PathItem.HttpMethod.DELETE -> pathItem.delete = operation
                PathItem.HttpMethod.HEAD -> pathItem.head = operation
                PathItem.HttpMethod.OPTIONS -> pathItem.options = operation
                PathItem.HttpMethod.PATCH -> pathItem.patch = operation
                PathItem.HttpMethod.TRACE -> pathItem.trace = operation
            }
        }

        val newPaths = io.swagger.v3.oas.models.Paths()
        newPaths.putAll(sortedPaths)
        openApi.paths = newPaths
    }

    fun sortSecurityRequirements(openApi: OpenAPI) {
        openApi.paths.values.forEach { pathItem ->
            pathItem.readOperations().forEach { op ->
                val headers = mutableMapOf<String, List<String>>()
                val cookies = mutableMapOf<String, List<String>>()

                op.security?.forEach { secRec ->
                    secRec.forEach { (name, scopes) ->
                        when {
                            name.contains("Header", ignoreCase = true) -> headers[name] = scopes
                            name.contains("Cookie", ignoreCase = true) -> cookies[name] = scopes
                        }
                    }
                }

                op.security = listOf(
                    SecurityRequirement().apply { headers.forEach { (k, v) -> addList(k, v)} },
                    SecurityRequirement().apply { cookies.forEach { (k, v) -> addList(k, v) }}
                )
            }
        }
    }
}
