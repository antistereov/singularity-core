package io.stereov.singularity.auth.group.dto.request

import io.stereov.singularity.auth.group.model.GroupTranslation
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

data class CreateGroupRequest(
    @field:Schema(description = "A unique key for the group.", example = "pilots")
    val key: String,

    @field:Schema(
        description = "A map of locale to group translation.",
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
)
