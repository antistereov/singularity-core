package io.stereov.singularity.auth.group.mapper

import io.stereov.singularity.auth.group.dto.request.CreateGroupRequest
import io.stereov.singularity.auth.group.dto.response.GroupResponse
import io.stereov.singularity.auth.group.model.GroupDocument
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.stereotype.Component
import java.util.*

@Component
class GroupMapper(
    private val translateService: TranslateService
) {

    fun createGroup(req: CreateGroupRequest): GroupDocument {

        return GroupDocument(
            key = req.key,
            translations = req.translations,
        )
    }

    suspend fun createGroupResponse(group: GroupDocument, locale: Locale?): GroupResponse {
        val (translated, content) = translateService.translate(group, locale)

        return GroupResponse(
            key = group.key,
            locale = translated,
            name = content.name,
            description = content.description
        )
    }
}
