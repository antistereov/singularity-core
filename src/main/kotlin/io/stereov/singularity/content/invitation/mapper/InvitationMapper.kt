package io.stereov.singularity.content.invitation.mapper

import io.stereov.singularity.content.invitation.dto.InvitationResponse
import io.stereov.singularity.content.invitation.model.Invitation
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
     * @return The successfully created [InvitationResponse].
     */
    fun toInvitationResponse(invitation: Invitation): InvitationResponse {
        return InvitationResponse(
            invitation.id,
            invitation.issuedAt,
            invitation.expiresAt,
            invitation.sensitive.email,
            invitation.sensitive.claims
        )
    }
}