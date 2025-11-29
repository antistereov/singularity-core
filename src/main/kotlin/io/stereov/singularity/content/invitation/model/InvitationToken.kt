package io.stereov.singularity.content.invitation.model

import io.stereov.singularity.global.model.Token
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Represents a token used for managing and validating invitation-related operations.
 *
 * The `InvitationToken` class is a specialized implementation of the abstract `Token` class,
 * uniquely associated with an invitation in the system. It encapsulates the invitation's
 * identification and its associated JWT (JSON Web Token) for secure handling and verification.
 *
 * This token primarily serves to securely convey and validate details associated with
 * an invitation, such as its validity, expiration, and identity in the system.
 *
 * @property invitationId The unique identifier of the invitation tied to this token.
 * @property jwt The JSON Web Token providing secure content and validation for the invitation.
 */
data class InvitationToken(
    val invitationId: ObjectId,
    override val jwt: Jwt
) : Token()
