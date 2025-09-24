package io.stereov.singularity.user.core.controller

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.model.FileNotFoundException
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
@Tag(
    name = "Users",
    description = "Operations related to user information."
)
class UserController(
    private val userService: UserService,
    private val authorizationService: AuthorizationService,
    private val userMapper: UserMapper,
    private val fileStorage: FileStorage
) {

    @GetMapping("/me")
    @Operation(
        summary = "Get currently authenticated user",
        description = "Retrieves the user profile information of the currently authenticated user.",
        security = [
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(name = OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "User found.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized. No valid token provided.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun getUser(): ResponseEntity<UserResponse> {
        val user = authorizationService.getCurrentUser()

        return ResponseEntity.ok(userMapper.toResponse(user))
    }

    @GetMapping("/{id}/avatar")
    suspend fun getAvatar(
        @PathVariable id: ObjectId
    ): ResponseEntity<FileMetadataResponse> {

        val user = userService.findById(id)
        val file = user.sensitive.avatarFileKey
            ?.let { fileStorage.metadataResponseByKey(it) }
            ?: throw FileNotFoundException(file = null, "No avatar set for user")

        return ResponseEntity.ok().body(file)
    }
}
