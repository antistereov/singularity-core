package io.stereov.singularity.auth.group.dto.request

import io.stereov.singularity.auth.group.model.GroupTranslation
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

data class UpdateGroupRequest(
    @field:Schema(
        description = "A map of locale to group translation. The translations will be added or updated.",
        example = """
            {
                "en": {
                    "name": "Pilots",
                    "description": "People who can fly"
                },
                "de": {
                    "name": "Piloten",
                    "description": "Personen, die fliegen können"
                }
            }
        """
    )
    val translations: MutableMap<Locale, GroupTranslation>,

    @field:Schema(description = "The translations to delete.", example = "en")
    val translationsToDelete: Set<Locale> = emptySet()
)