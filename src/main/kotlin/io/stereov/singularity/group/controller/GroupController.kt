package io.stereov.singularity.group.controller

import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.group.dto.CreateGroupRequest
import io.stereov.singularity.group.dto.GroupResponse
import io.stereov.singularity.group.dto.UpdateGroupRequest
import io.stereov.singularity.group.service.GroupService
import io.stereov.singularity.translate.model.Language
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
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

    @GetMapping
    suspend fun findGroups(
        @RequestParam page: Int = 0,
        @RequestParam size: Int = 10,
        @RequestParam sort: List<String> = emptyList(),
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<Page<GroupResponse>> {
        return ResponseEntity.ok(
            service.findAllPaginated(page, size, sort, lang = lang).map { it.toResponse(lang) }
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

    @PutMapping
    suspend fun updateGroup(
        @RequestBody req: UpdateGroupRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<GroupResponse> {
        return ResponseEntity.ok(
            service.update(req).toResponse(lang)
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
