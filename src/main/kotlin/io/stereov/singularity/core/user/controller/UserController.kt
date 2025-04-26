package io.stereov.singularity.core.user.controller

import io.stereov.singularity.core.global.service.file.model.FileMetaData
import io.stereov.singularity.core.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/user")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/{id}/avatar")
    suspend fun getAvatar(
        @PathVariable id: String
    ): ResponseEntity<FileMetaData> {
        return ResponseEntity.ok().body(userService.getAvatar(id))
    }
}
