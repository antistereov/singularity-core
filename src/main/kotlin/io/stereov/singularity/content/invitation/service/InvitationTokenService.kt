package io.stereov.singularity.content.invitation.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.content.invitation.exception.InvitationTokenCreationException
import io.stereov.singularity.content.invitation.exception.InvitationTokenExtractionException
import io.stereov.singularity.content.invitation.model.Invitation
import io.stereov.singularity.content.invitation.model.InvitationToken
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service

/**
 * Service responsible for handling operations related to invitation tokens, including creation
 * and extraction of tokens. Invitation tokens are represented as JSON Web Tokens (JWTs) and
 * are used to securely manage invitation-related activities.
 *
 * @property jwtService Service used for JWT encoding and decoding operations.
 * @property logger Logger instance for debugging and logging purposes.
 * @property tokenType Represents the type for identifying invitation tokens during JWT operations.
 */
@Service
class InvitationTokenService(
    private val jwtService: JwtService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}
    private val tokenType = "invitation_token"

    /**
     * Creates an invitation token based on the provided invitation details.
     *
     * This method generates a JWT (JSON Web Token) for the given invitation, encapsulating its ID,
     * issued timestamp, and expiration timestamp. The generated token can be securely used for
     * managing and validating the invitation-related operations.
     *
     * @param invitation The invitation object containing the details necessary to create the token, such as issued and expiry timestamps, and the unique ID.
     * @return A [Result] containing the successfully created [InvitationToken] or an [InvitationTokenCreationException] in case of failure during the token creation process.
     */
    suspend fun create(
        invitation: Invitation
    ): Result<InvitationToken, InvitationTokenCreationException> = coroutineBinding {
        logger.debug { "Creating invitation token" }

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(invitation.issuedAt)
                .expiresAt(invitation.expiresAt)
                .subject(invitation.id.toString())
                .build()
        }
            .mapError { ex -> InvitationTokenCreationException.Encoding("Failed to create JWT claims: ${ex.message}", ex) }
            .bind()

        val jwt = jwtService.encodeJwt(claims, tokenType)
            .mapError { ex -> InvitationTokenCreationException.from(ex) }
            .bind()

        val id = invitation.id
            .mapError { ex -> InvitationTokenCreationException.InvalidInvitation("Failed to extract ID from invitation: ${ex.message}", ex) }
            .bind()

        InvitationToken(id, jwt)
    }

    /**
     * Extracts an invitation token from a given JWT token string.
     *
     * This method validates and decodes the provided JWT to extract the associated
     * invitation details and encapsulates them into an [InvitationToken] object.
     * If the validation or decoding process fails, an [InvitationTokenExtractionException] is returned.
     *
     * @param token The JWT token string to be validated and decoded for extracting the invitation details.
     * @return A [Result] containing the successfully extracted [InvitationToken] or an [InvitationTokenExtractionException]
     * in case of failure during the validation or decoding process.
     */
    suspend fun extract(
        token: String
    ): Result<InvitationToken, InvitationTokenExtractionException> = coroutineBinding {
        logger.debug { "Validating invitation token" }

        val jwt = jwtService.decodeJwt(token, tokenType)
            .mapError { InvitationTokenExtractionException.from(it) }
            .bind()

        InvitationToken(ObjectId(jwt.subject), jwt)
    }
}
