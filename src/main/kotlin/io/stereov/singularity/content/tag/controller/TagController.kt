package io.stereov.singularity.content.tag.controller

import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.content.tag.dto.KeyContainsResponse
import io.stereov.singularity.content.tag.dto.TagResponse
import io.stereov.singularity.content.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.global.model.SuccessResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/content/tags")
class TagController(
    private val service: TagService,
    private val tagMapper: TagMapper
) {

    @PostMapping
    suspend fun createTag(@RequestBody req: CreateTagRequest): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(
            tagMapper.createTagResponse(service.create(req), req.locale)
        )
    }

    @GetMapping("/{key}")
    suspend fun findTagByKey(
        @PathVariable key: String,
        @RequestParam locale: Locale?
    ): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(
            tagMapper.createTagResponse(service.findByKey(key), locale)
        )
    }

    @GetMapping
    suspend fun findTagByKeyContains(
        @RequestParam substring: String,
        @RequestParam locale: Locale?,
    ): ResponseEntity<KeyContainsResponse> {
        val tagList = service.findByNameContains(substring, locale)

        return ResponseEntity.ok(
            KeyContainsResponse(tagList.map { tagMapper.createTagResponse(it, locale) }, tagList.size)
        )
    }

    @PutMapping("/{key}")
    suspend fun updateTag(
        @PathVariable key: String,
        @RequestBody req: UpdateTagRequest,
        @RequestParam locale: Locale?
    ): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(tagMapper.createTagResponse(service.updateTag(key, req), locale))
    }

    @DeleteMapping("/{key}")
    suspend fun deleteTag(@PathVariable key: String): ResponseEntity<SuccessResponse> {
        return ResponseEntity.ok(SuccessResponse(service.deleteByKey(key)))
    }

}
