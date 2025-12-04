package io.stereov.singularity.content.invitation.model

/**
 * Represents sensitive data associated with an invitation.
 *
 * This data class contains information that must be securely handled during the processing
 * of invitations. The sensitive data includes the recipient's email address and a set of
 * claims associated with the invitation. Claims can represent additional metadata or context
 * pertaining to the invitation.
 *
 * Instances of this class are used as part of invitation models to encapsulate information
 * that is sensitive and may require secure handling or encryption, depending on the use case.
 *
 * @property email The email address of the recipient associated with the invitation.
 * @property claims A map of key-value pairs representing relevant metadata or additional
 * attributes associated with the invitation.
 */
data class SensitiveInvitationData(
    val email: String,
    val claims: Map<String, Any>
)
