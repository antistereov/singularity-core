package io.stereov.singularity.principal.core.mapper

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.recover
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.util.getOrNull
import io.stereov.singularity.principal.core.dto.response.PrincipalResponse
import io.stereov.singularity.principal.core.dto.response.PrincipalOverviewResponse
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.model.Guest
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.identity.UserIdentity
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import org.springframework.stereotype.Service

@Service
class PrincipalMapper(
    private val fileStorage: FileStorage
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Converts a given user and authentication outcome into a PrincipalResponse object encapsulated
     * within a [Result] type, representing either a successful response or an error.
     *
     * @param principal The [Principal] object containing user details to be mapped.
     * @param authenticationOutcome The [AuthenticationOutcome] providing context for the mapping process.
     * @return A [Result] containing a [PrincipalResponse] object when successful, or a [PrincipalMapperException] in case of failure.
     */
    suspend fun toResponse(
        principal: Principal<out Role, out SensitivePrincipalData>,
        authenticationOutcome: AuthenticationOutcome = AuthenticationOutcome.None()
    ): Result<PrincipalResponse, PrincipalMapperException> = coroutineBinding {
        logger.debug { "Creating PrincipalResponse for user with id \"${principal.id}\"" }
        
        val avatarKey = when (principal) {
            is User -> principal.sensitive.avatarFileKey
            is Guest -> null
        }

        val avatarMetadata = avatarKey
            ?.let { fileStorage.metadataResponseByKey(it, authenticationOutcome) }
            ?.recover { null }
            ?.bind()
        
        val id = principal.id.mapError { ex -> PrincipalMapperException.InvalidDocument("Failed to create user response because the user document does not contain an id: ${ex.message}", ex) }
            .bind()
        
        val identityProviders = when (principal) {
            is User -> principal.sensitive.identities.providers.map { it.key }
                .toMutableList()
                .apply {
                    if (principal.sensitive.identities.password != null) add(UserIdentity.PASSWORD_IDENTITY)
                }
            is Guest -> emptyList()
        }

        val email = when (principal) {
            is User -> principal.email
            is Guest -> null
        }

        val emailVerified = when (principal) {
            is User -> principal.sensitive.security.email.verified
            is Guest -> false
        }

        val twoFactorEnabled = when (principal) {
            is User -> principal.twoFactorEnabled
            is Guest -> false
        }

        val preferredTwoFactorMethod = when (principal) {
            is User -> principal.preferredTwoFactorMethod.getOrNull()
            is Guest -> null
        }

        val twoFactorMethods = when (principal) {
            is User -> principal.twoFactorMethods
            is Guest -> emptyList()
        }

        PrincipalResponse(
            id,
            principal.sensitive.name,
            email,
            identityProviders,
            principal.roles,
            emailVerified,
            principal.lastActive.toString(),
            twoFactorEnabled,
            preferredTwoFactorMethod,
            twoFactorMethods,
            avatarMetadata,
            principal.createdAt.toString(),
            principal.groups
        )
    }

    /**
     * Converts a given user and authentication outcome into a PrincipalOverviewResponse object
     * encapsulated within a [Result] type, representing either a successful response or an error.
     *
     * @param principal The [Principal] object containing the user details to be mapped into an overview response.
     * @param authenticationOutcome The [AuthenticationOutcome] providing context for retrieving associated data.
     * @return A [Result] containing a [PrincipalOverviewResponse] when successfully mapped, or a [PrincipalMapperException]
     * in case of a mapping failure.
     */
    suspend fun toOverview(
        principal: Principal<out Role, out SensitivePrincipalData>,
        authenticationOutcome: AuthenticationOutcome
    ): Result<PrincipalOverviewResponse, PrincipalMapperException> = coroutineBinding {
        logger.debug { "Creating PrincipalOverviewResponse for user with ID \"${principal.id}\"" }

        val avatarKey = when (principal) {
            is User -> principal.sensitive.avatarFileKey
            is Guest -> null
        }

        val avatarMetadata = avatarKey
            ?.let { fileStorage.metadataResponseByKey(it, authenticationOutcome) }
            ?.recover { ex ->
                logger.error(ex) { "Failed to get avatar key" }
                null
            }
            ?.bind()
        
        val id = principal.id
            .mapError { ex -> PrincipalMapperException.InvalidDocument("Failed to create user overview response because the user document does not contain an id: ${ex.message}", ex) }
            .bind()
        
        PrincipalOverviewResponse(
            id,
            principal.sensitive.name,
            avatarMetadata,
            principal.roles
        )
    }
}
