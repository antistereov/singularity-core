package io.stereov.singularity.content.invitation.mapper

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import io.stereov.singularity.content.invitation.dto.InvitationResponse
import io.stereov.singularity.content.invitation.model.Invitation
import io.stereov.singularity.database.core.exception.DocumentException
import org.springframework.stereotype.Component

/**
 * Responsible for mapping between [Invitation] and [InvitationResponse] objects.
 *
 * The `InvitationMapper` encapsulates the logic required to transform an [Invitation] entity
 * into a corresponding [InvitationResponse]. It handles the mapping of essential fields, including
 * sensitive data, and provides a clear encapsulation of the transformation logic.
 * This class is a Spring component and can be injected wherever necessary.
 */
@Component
class InvitationMapper {

    /**
     * Converts an [Invitation] object into an [InvitationResponse].
     *
     * The method maps the [Invitation] object fields to create a corresponding [InvitationResponse], using
     * the id, issuedAt, expiresAt, and sensitive data such as email and claims.
     *
     * @param invitation The Invitation object containing the details to be converted.
     * @return A [Result] containing the successfully created [InvitationResponse] or a [DocumentException.Invalid]
     * indicating an error in case of an invalid document.
     */
    fun toInvitationResponse(invitation: Invitation): Result<InvitationResponse, DocumentException.Invalid> {
        return invitation.id.map { id ->
            InvitationResponse(
                id,
                invitation.issuedAt,
                invitation.expiresAt,
                invitation.sensitive.email,
                invitation.sensitive.claims
            )
        }
    }
}