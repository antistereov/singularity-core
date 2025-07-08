package io.stereov.singularity.content.common.tag.controller

import io.stereov.singularity.content.common.tag.dto.CreateTagRequest
import io.stereov.singularity.content.common.tag.dto.KeyContainsResponse
import io.stereov.singularity.content.common.tag.dto.TagResponse
import io.stereov.singularity.content.common.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.common.tag.service.TagService
import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.global.model.SuccessResponse
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
        return ResponseEntity.ok(service.create(req).toResponse(req.lang))
    }

    @GetMapping("/{key}")
    suspend fun findByKey(
        @PathVariable key: String,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(service.findByKey(key).toResponse(lang))
    }

    @GetMapping
    suspend fun findByKeyContains(
        @RequestParam substring: String,
        @RequestParam lang: Language = Language.EN,
    ): ResponseEntity<KeyContainsResponse> {
        val tagList = service.findByNameContains(substring, lang)

        return ResponseEntity.ok(
            KeyContainsResponse(tagList.map { it.toResponse(lang) }, tagList.size)
        )
    }

    @PutMapping("/{key}")
    suspend fun updateTag(
        @PathVariable key: String,
        @RequestBody req: UpdateTagRequest,
        @RequestParam lang: Language = Language.EN
    ): ResponseEntity<TagResponse> {
        return ResponseEntity.ok(service.updateTag(key, req).toResponse(lang))
    }

    @DeleteMapping("/{key}")
    suspend fun deleteTag(@PathVariable key: String): ResponseEntity<SuccessResponse> {
        return ResponseEntity.ok(SuccessResponse(service.deleteByKey(key)))
    }

}
