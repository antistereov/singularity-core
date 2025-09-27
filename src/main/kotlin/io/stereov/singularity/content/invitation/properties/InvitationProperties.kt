package io.stereov.singularity.content.invitation.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.content.invitations")
data class InvitationProperties(
    val acceptUrl: String = "http://localhost:4200/content/{contentType}/{contentKey}/accept-invitation",
    val allowUnregisterdUsers: Boolean = true
)