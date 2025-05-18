package io.stereov.singularity.content.common.tag.controller

import io.stereov.singularity.content.common.tag.dto.CreateTagRequest
import io.stereov.singularity.content.common.tag.dto.NameContainsResponse
import io.stereov.singularity.content.common.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.common.tag.model.TagDocument
import io.stereov.singularity.content.common.tag.service.TagService
import io.stereov.singularity.core.global.model.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/api/content/tags")
class TagController(
    private val service: TagService
) {

    @PostMapping
    suspend fun create(@RequestBody req: CreateTagRequest): ResponseEntity<TagDocument> {
        return ResponseEntity.ok(service.create(req))
    }

    @GetMapping
    suspend fun findNameContains(@RequestParam substring: String): ResponseEntity<NameContainsResponse> {
        val tagList = service.findNameContains(substring)

        return ResponseEntity.ok(
            NameContainsResponse(tagList, tagList.size)
        )
    }

    @PutMapping("/{id}")
    suspend fun updateTag(@PathVariable id: String, @RequestBody req: UpdateTagRequest): ResponseEntity<TagDocument> {
        return ResponseEntity.ok(service.updateTag(id, req))
    }

    @DeleteMapping("/{id}")
    suspend fun deleteTag(@PathVariable id: String): ResponseEntity<SuccessResponse> {
        return ResponseEntity.ok(SuccessResponse(service.deleteById(id)))
    }

}
