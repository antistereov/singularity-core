package io.stereov.singularity.content.common.tag.controller

import io.stereov.singularity.content.common.tag.dto.CreateTagRequest
import io.stereov.singularity.content.common.tag.dto.KeyContainsResponse
import io.stereov.singularity.content.common.tag.dto.TagResponse
import io.stereov.singularity.content.common.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.common.tag.service.TagService
import io.stereov.singularity.core.global.model.SuccessResponse
import org.bson.types.ObjectId
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/api/content/tags")
class TagController(
    private val service: TagService
) {

    @PostMapping
    suspend fun create(@RequestBody req: CreateTagRequest): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(service.create(req).toResponse())
    }

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable id: String): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(service.findById(ObjectId(id)).toResponse())
    }

    @GetMapping
    suspend fun findByKeyContains(@RequestParam substring: String): ResponseEntity<KeyContainsResponse> {
        val tagList = service.findByKeyContains(substring)

        return ResponseEntity.ok(
            KeyContainsResponse(tagList.map { it.toResponse() }, tagList.size)
        )
    }

    @PutMapping("/{id}")
    suspend fun updateTag(@PathVariable id: String, @RequestBody req: UpdateTagRequest): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(service.updateTag(id, req).toResponse())
    }

    @DeleteMapping("/{id}")
    suspend fun deleteTag(@PathVariable id: String): ResponseEntity<SuccessResponse> {
        return ResponseEntity.ok(SuccessResponse(service.deleteById(id)))
    }

}
