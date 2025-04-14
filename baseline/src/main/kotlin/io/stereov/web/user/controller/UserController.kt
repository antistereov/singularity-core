package io.stereov.web.user.controller

import io.stereov.web.user.service.UserService
import org.springframework.core.io.InputStreamResource
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
    ): ResponseEntity<InputStreamResource> {
        return userService.getAvatar(id).toResponseEntity()
    }
}
