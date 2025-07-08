package io.stereov.singularity.user.controller

import io.stereov.singularity.file.core.model.FileMetaData
import io.stereov.singularity.user.service.UserService
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/{id}/avatar")
    suspend fun getAvatar(
        @PathVariable id: String
    ): ResponseEntity<FileMetaData> {
        return ResponseEntity.ok().body(userService.getAvatar(ObjectId(id)))
    }
}
