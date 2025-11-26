package io.stereov.singularity.principal.group.mapper

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.stereov.singularity.principal.group.dto.request.CreateGroupRequest
import io.stereov.singularity.principal.group.dto.response.GroupResponse
import io.stereov.singularity.principal.group.model.Group
import io.stereov.singularity.translate.exception.TranslateException
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.stereotype.Component
import java.util.*

/**
 * A mapper class for converting between [Group]-related entities and responses.
 * This class is responsible for creating a Group entity from a request and generating
 * localized GroupResponse objects based on available translations and a specified locale.
 */
@Component
class GroupMapper(
    private val translateService: TranslateService
) {

    /**
     * Creates a [Group] entity from the provided request.
     *
     * The method takes a CreateGroupRequest containing the key and translations for the group
     * and constructs a Group instance. The translations are stored in a map with locales as keys
     * and GroupTranslation objects as values.
     *
     * @param req The [CreateGroupRequest] object containing details for the group, including the unique key
     *   and the map of translations by locale.
     * @return A [Group] instance containing the provided key and translations.
     */
    fun createGroup(req: CreateGroupRequest): Group {

        return Group(
            key = req.key,
            translations = req.translations,
        )
    }

    /**
     * Creates a response for a given group by translating it to the specified locale.
     * This method uses a translation service to find the best matching translations for the provided group
     * and locale, and constructs a GroupResponse containing the translations.
     *
     * @param group The group entity containing translations and a unique identifier.
     * @param locale The desired locale for translation. If null, default or available translations
     *               may be used instead.
     * @return A [Result] containing a successful [GroupResponse] for the specified locale, or a
     * [TranslateException.NoTranslations] if no translations are available.
     */
    suspend fun createGroupResponse(
        group: Group, 
        locale: Locale?
    ): Result<GroupResponse, TranslateException.NoTranslations> = coroutineBinding {
        val (translated, content) = translateService.translate(group, locale)
            .bind()

        GroupResponse(
            key = group.key,
            locale = translated,
            name = content.name,
            description = content.description
        )
    }
}
