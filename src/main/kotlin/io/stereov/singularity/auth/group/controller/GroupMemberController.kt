package io.stereov.singularity.auth.group.controller

import io.stereov.singularity.auth.group.dto.request.AddGroupMemberRequest
import io.stereov.singularity.auth.group.service.GroupMemberService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Groups")
class GroupMemberController(
    private val groupMemberService: GroupMemberService,
    private val userMapper: UserMapper
) {

    @PostMapping("/{groupKey}/members")
    @Operation(
        summary = "Add Member to Group",
        description = "Add a member to a group. Invalidates all AccessTokens.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#adding-members-to-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid token.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User does not have ADMIN role.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun addMemberToGroup(
        @PathVariable groupKey: String,
        @RequestBody req: AddGroupMemberRequest
    ): ResponseEntity<UserResponse> {
        val user = groupMemberService.add(req.userId, groupKey)

        return ResponseEntity.ok(userMapper.toResponse(user))
    }

    @DeleteMapping("/{groupKey}/members/{userId}")
    @Operation(
        summary = "Remove Member from Group",
        description = "Remove a member from a group. Invalidates all AccessTokens.",
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/groups#removing-members-from-groups"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),

        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Updated user information.",
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid token.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "User does not have ADMIN role.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun removeMemberFromGroup(
        @PathVariable groupKey: String,
        @PathVariable userId: ObjectId
    ): ResponseEntity<UserResponse> {
        val user = groupMemberService.remove(userId, groupKey)

        return ResponseEntity.ok(userMapper.toResponse(user))
    }
}
