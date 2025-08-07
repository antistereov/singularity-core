package io.stereov.singularity.invitation.controller

import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.invitation.service.InvitationService
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/invite")
class InvitationController(
    private val service: InvitationService
) {

    @DeleteMapping("/{id}")
    suspend fun deleteInvitation(@PathVariable id: ObjectId): ResponseEntity<SuccessResponse> {
        service.deleteById(id)
        return ResponseEntity.ok(SuccessResponse(true))
    }
}
