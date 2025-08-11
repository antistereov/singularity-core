package io.stereov.singularity.user.core.controller

import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/{id}/avatar")
    suspend fun getAvatar(
        @PathVariable id: String
    ): ResponseEntity<FileMetadataResponse> {
        return ResponseEntity.ok().body(userService.getAvatar(ObjectId(id)))
    }
}
