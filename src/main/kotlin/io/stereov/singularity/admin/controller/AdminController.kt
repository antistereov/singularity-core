package io.stereov.singularity.admin.controller

import io.stereov.singularity.admin.dto.RotationStatusResponse
import io.stereov.singularity.admin.service.AdminService
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.user.dto.UserResponse
import io.stereov.singularity.user.service.UserService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api/admin")
class AdminController(
    private val adminService: AdminService,
    private val userService: UserService,
) {

    @PostMapping("/rotate-keys")
    suspend fun rotateKeys(): ResponseEntity<SuccessResponse> = coroutineScope {
        async { adminService.rotateKeys() }.start()

        return@coroutineScope ResponseEntity.ok(SuccessResponse(true))
    }

    @GetMapping("/rotate-keys/status")
    suspend fun rotationOngoing(): ResponseEntity<RotationStatusResponse> {
        return ResponseEntity.ok(
            this.adminService.getRotationStatus()
        )
    }

    @GetMapping("/users")
    suspend fun getAllUsers(): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(
            userService.findAll().map { user -> userService.createResponse(user) }.toList()
        )
    }
}
