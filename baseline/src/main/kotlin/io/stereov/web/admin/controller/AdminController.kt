package io.stereov.web.admin.controller

import io.stereov.web.admin.dto.RotationStatusResponse
import io.stereov.web.admin.service.AdminService
import io.stereov.web.global.model.SuccessResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin")
class AdminController(
    private val adminService: AdminService
) {

    @PostMapping("/rotate-keys")
    suspend fun rotateKeys(): ResponseEntity<SuccessResponse> = coroutineScope {
        async { adminService.rotateKeys() }.start()

        return@coroutineScope ResponseEntity.ok(SuccessResponse(true))
    }

    @GetMapping("/rotate-keys/status")
    suspend fun rotationOngoing(): ResponseEntity<RotationStatusResponse> {
        return ResponseEntity.ok(
            RotationStatusResponse(this.adminService.rotationOngoing())
        )
    }
}
