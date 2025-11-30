package io.stereov.singularity.global.config

import io.stereov.singularity.admin.core.controller.AdminController
import io.stereov.singularity.auth.core.controller.AuthenticationController
import io.stereov.singularity.auth.core.controller.EmailVerificationController
import io.stereov.singularity.auth.core.controller.PasswordResetController
import io.stereov.singularity.auth.core.controller.SessionController
import io.stereov.singularity.auth.oauth2.controller.IdentityProviderController
import io.stereov.singularity.auth.oauth2.controller.OAuth2ProviderController
import io.stereov.singularity.auth.token.model.SessionTokenType
import io.stereov.singularity.auth.token.model.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.controller.EmailAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TotpAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TwoFactorAuthenticationController
import io.stereov.singularity.content.article.controller.ArticleController
import io.stereov.singularity.content.article.controller.ArticleManagementController
import io.stereov.singularity.content.core.controller.ContentManagementController
import io.stereov.singularity.content.invitation.controller.InvitationController
import io.stereov.singularity.content.tag.controller.TagController
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.principal.core.controller.GuestController
import io.stereov.singularity.principal.core.controller.UserController
import io.stereov.singularity.principal.group.controller.GroupController
import io.stereov.singularity.principal.group.controller.GroupMemberController
import io.stereov.singularity.principal.settings.controller.PrincipalSettingsController
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import kotlin.reflect.full.primaryConstructor

@AutoConfiguration
@SecurityScheme(
    name = OpenApiConstants.ACCESS_TOKEN_HEADER,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Access token for user authentication. " +
            "You can learn more [here](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)."
)
@SecurityScheme(
    name = OpenApiConstants.ACCESS_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = SessionTokenType.Access.COOKIE_NAME,
    description = "Access token for user authentication." +
            "You can learn more [here](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)."
)

@SecurityScheme(
    name = OpenApiConstants.REFRESH_TOKEN_HEADER,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Refresh token to request new access tokens." +
            "You can learn more [here](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)."
)
@SecurityScheme(
    name = OpenApiConstants.REFRESH_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = SessionTokenType.Refresh.COOKIE_NAME,
    description = "Refresh token to request new access tokens." +
            "You can learn more [here](https://singularity.stereov.io/docs/guides/auth/tokens#refresh-token)."
)

@SecurityScheme(
    name = OpenApiConstants.STEP_UP_TOKEN_HEADER,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = SessionTokenType.StepUp.HEADER,
    description = "EmailVerificationTokenCreation for step up authentication allowing access of secure resources." +
            "You can learn more [here](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)."
)
@SecurityScheme(
    name = OpenApiConstants.STEP_UP_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = SessionTokenType.StepUp.COOKIE_NAME,
    description = "EmailVerificationTokenCreation for step up authentication allowing access of secure resources." +
            "You can learn more [here](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)."
)

@SecurityScheme(
    name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_HEADER,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.HEADER,
    paramName = TwoFactorTokenType.Authentication.HEADER,
    description = "EmailVerificationTokenCreation for successful authentication with email and password, indicating 2FA is required." +
            "You can learn more [here](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token)."
)
@SecurityScheme(
    name = OpenApiConstants.TWO_FACTOR_AUTHENTICATION_TOKEN_COOKIE,
    type = SecuritySchemeType.APIKEY,
    `in` = SecuritySchemeIn.COOKIE,
    paramName = TwoFactorTokenType.Authentication.COOKIE_NAME,
    description = "EmailVerificationTokenCreation for successful authentication with email and password, indicating 2FA is required." +
            "You can learn more [here](https://singularity.stereov.io/docs/guides/auth/tokens#two-factor-authentication-token)."
)

class OpenApiConfig() {

    @Bean
    fun domainErrorOperationCustomizer(): OperationCustomizer = DomainErrorOperationCustomizer()

    @Component
    class DomainErrorOperationCustomizer : OperationCustomizer {

        override fun customize(operation: Operation, handlerMethod: HandlerMethod): Operation {
            val domainErrorAnnotation = handlerMethod.getMethodAnnotation(ThrowsDomainError::class.java)
                ?: return operation

            domainErrorAnnotation.errorClasses.forEach { errorKClass ->

                errorKClass.sealedSubclasses.forEach { concreteErrorClass ->

                    val apiError = concreteErrorClass.objectInstance
                        ?: concreteErrorClass.primaryConstructor?.call("", null)
                        ?: return@forEach

                    val responseCode = apiError.status.value().toString()
                    val descriptionEntry = "\n* **`${apiError.code}`:** ${apiError.description}"

                    val existingResponse = operation.responses[responseCode]
                    if (existingResponse == null) {
                        val initialDescription = "The following error codes correspond to this status:$descriptionEntry"

                        operation.responses.addApiResponse(
                            responseCode,
                            ApiResponse()
                                .description(initialDescription)
                                .content(createErrorContent())
                        )
                    } else {
                        val currentDescription = existingResponse.description ?: "A specific domain error occurred (see list below):"
                        existingResponse.description = if (currentDescription.contains("`${apiError.code}`:")) {
                            currentDescription
                        } else {
                            currentDescription + descriptionEntry
                        }
                    }
                }
            }

            return operation
        }

        private fun createErrorContent(): Content {
            return Content().addMediaType(
                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                io.swagger.v3.oas.models.media.MediaType()
                    .schema(Schema<ErrorResponse>())
            )
        }
    }


    @Bean
    fun customize(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        trimIndents(openApi)
        sortSecurityRequirements(openApi)
        sortTags(openApi)
        sortEndpoints(openApi)
    }

    private fun trimIndents(openApi: OpenAPI) {
        openApi.info.summary = openApi.info.summary?.trimIndent()
        openApi.info.description = openApi.info.description?.trimIndent()

        openApi.components.schemas.forEach { (_, v) ->
            v.name = v.name?.trimIndent()
            v.description = v.description?.trimIndent()
        }

        openApi.paths.forEach { (_, pathItem) ->
            pathItem.readOperations().forEach { operation ->
                operation.summary = operation.summary?.trimIndent()
                operation.description = operation.description?.trimIndent()
            }
        }
    }

    private fun sortTags(openAPI: OpenAPI) {
        val sortedTagNames = listOf(
            "Authentication",
            "Two-Factor Authentication",
            "OAuth2",
            "Sessions",
            "Roles",
            "Groups",
            "Managing Users",
            "Profile Management",
            "Security",
            "Articles",
            "Content Management",
            "Invitations",
            "Tags",
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

            TwoFactorAuthenticationController::changePreferredTwoFactorMethod.name,

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

        // Roles

        sortedOperationIds.addAll(listOf(
            GuestController::createGuestAccount.name,
            GuestController::convertGuestToUser.name,

            AdminController::grantAdminPermissions.name,
            AdminController::revokeAdminPermissions.name
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

        // Managing Users

        sortedOperationIds.addAll(listOf(
            UserController::getUserById.name,
            UserController::getUsers.name,
            UserController::deleteUserById.name,
        ))

        // Profile Management

        sortedOperationIds.addAll(listOf(
            PrincipalSettingsController::getAuthorizedPrincipal.name,
            PrincipalSettingsController::updateAuthorizedUser.name,
            PrincipalSettingsController::changeEmailOfAuthorizedUser.name,
            PrincipalSettingsController::changePasswordOfAuthorizedUser.name,
            PrincipalSettingsController::setAvatarOfAuthorizedUser.name,
            PrincipalSettingsController::deleteAvatarOfAuthorizedUser.name,
            PrincipalSettingsController::deleteAuthorizedPrincipal.name
        ))

        // Articles

        sortedOperationIds.addAll(listOf(
            ArticleManagementController::createArticle.name,

            ArticleController::getArticleByKey.name,
            ArticleController::getArticles.name,

            ArticleManagementController::updateArticle.name,
            ArticleManagementController::updateArticleState.name,
            ArticleManagementController::updateArticleImage.name,
        ))

        // Content Management

        sortedOperationIds.addAll(listOf(
            ContentManagementController::getContentObjectAccessDetails.name,
            ContentManagementController::updateContentObjectOwner.name,
            ContentManagementController::updateContentObjectAccess.name,
            ContentManagementController::updateContentObjectTrustedState.name,
            ContentManagementController::deleteContentObjectByKey.name
        ))

        // Tags

        sortedOperationIds.addAll(listOf(
            TagController::createTag.name,
            TagController::getTagByKey.name,
            TagController::getTags.name,
            TagController::updateTag.name,
            TagController::deleteTag.name,
        ))

        // Invitations

        sortedOperationIds.addAll(listOf(
            InvitationController::inviteUserToContentObject.name,
            InvitationController::acceptInvitationToContentObject.name,
            InvitationController::deleteInvitationToContentObjectById.name
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
        val doNotSort = listOf(
            EmailAuthenticationController::sendEmailTwoFactorCode.name,
            EmailAuthenticationController::getRemainingEmailTwoFactorCooldown.name
        )

        openApi.paths.values.forEach { pathItem ->
            pathItem.readOperations().forEach { op ->
                if (doNotSort.contains(op.operationId)) return@forEach

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
