package io.stereov.singularity.content.invitation.controller

import io.stereov.singularity.content.core.dto.request.AcceptInvitationToContentRequest
import io.stereov.singularity.content.core.dto.request.InviteUserToContentRequest
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.dto.response.ExtendedContentAccessDetailsResponse
import io.stereov.singularity.content.core.util.findContentManagementService
import io.stereov.singularity.content.invitation.service.InvitationService
import io.stereov.singularity.global.model.SuccessResponse
import org.bson.types.ObjectId
import org.springframework.context.ApplicationContext
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("api/content/invitations")
class InvitationController(
    private val service: InvitationService,
    private val context: ApplicationContext,
) {

    @PostMapping("/{contentType}/{key}")
    suspend fun inviteUser(
        @PathVariable key: String,
        @PathVariable contentType: String,
        @RequestBody req: InviteUserToContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<ExtendedContentAccessDetailsResponse> {
        return ResponseEntity.ok(
            context.findContentManagementService(contentType).inviteUser(key, req, locale)
        )
    }

    @PostMapping("/{contentType}/accept")
    suspend fun acceptInvitation(
        @PathVariable contentType: String,
        @RequestBody req: AcceptInvitationToContentRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<out ContentResponse<*>> {
        return ResponseEntity.ok(
            context.findContentManagementService(contentType).acceptInvitation(req, locale)
        )
    }

    @DeleteMapping("/{id}")
    suspend fun deleteInvitation(@PathVariable id: ObjectId): ResponseEntity<SuccessResponse> {
        service.deleteById(id)
        return ResponseEntity.ok(SuccessResponse(true))
    }


}
