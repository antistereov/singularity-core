package io.stereov.singularity.auth.group.controller

import io.stereov.singularity.auth.group.dto.request.AddGroupMemberRequest
import io.stereov.singularity.auth.group.service.GroupMemberService
import io.stereov.singularity.user.core.dto.response.UserResponse
import io.stereov.singularity.user.core.mapper.UserMapper
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/groups")
class GroupMemberController(
    private val groupMemberService: GroupMemberService,
    private val userMapper: UserMapper
) {

    @PostMapping("/{groupKey}/members")
    suspend fun addUser(
        @PathVariable groupKey: String,
        @RequestBody req: AddGroupMemberRequest
    ): ResponseEntity<UserResponse> {
        val user = groupMemberService.add(req.userId, groupKey)

        return ResponseEntity.ok(userMapper.toResponse(user))
    }

    @DeleteMapping("/{groupKey}/members/{userId}")
    suspend fun removeUser(
        @PathVariable groupKey: String,
        @PathVariable userId: ObjectId
    ): ResponseEntity<UserResponse> {
        val user = groupMemberService.remove(userId, groupKey)

        return ResponseEntity.ok(userMapper.toResponse(user))
    }
}
