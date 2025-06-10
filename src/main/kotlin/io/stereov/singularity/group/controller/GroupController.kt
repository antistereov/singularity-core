package io.stereov.singularity.group.controller

import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.group.dto.CreateGroupRequest
import io.stereov.singularity.group.dto.GroupResponse
import io.stereov.singularity.group.service.GroupService
import io.stereov.singularity.translate.model.Language
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("api/groups")
class GroupController(
    private val service: GroupService
) {

    @PostMapping
    suspend fun create(
        @RequestBody req: CreateGroupRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<GroupResponse> {
        return ResponseEntity.ok(
            service.create(req).toResponse(lang)
        )
    }

    @GetMapping("/{key}")
    suspend fun findByKey(
        @PathVariable key: String,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<GroupResponse> {
        return ResponseEntity.ok(
            service.findByKey(key).toResponse(lang)
        )
    }

    @PutMapping("/{key}")
    suspend fun updateGroup(
        @PathVariable key: String,
        @RequestBody req: UpdateGroupRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<GroupResponse> {
        return ResponseEntity.ok(
            service.update(key, req).toResponse(lang)
        )
    }

    @DeleteMapping("/{key}")
    suspend fun deleteGroup(
        @PathVariable key: String
    ): ResponseEntity<SuccessResponse> {
        service.deleteByKey(key)
        return ResponseEntity.ok(SuccessResponse())
    }
}
