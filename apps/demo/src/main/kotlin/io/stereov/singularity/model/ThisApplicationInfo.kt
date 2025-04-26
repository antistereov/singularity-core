package io.stereov.singularity.model

import io.stereov.singularity.core.user.dto.ApplicationInfoDto
import io.stereov.singularity.core.user.model.ApplicationInfo
import io.stereov.singularity.dto.ThisAppDto

data class ThisApplicationInfo(
    val favoriteColor: String
) : ApplicationInfo {

    override fun toDto(): ApplicationInfoDto {
        return ThisAppDto(favoriteColor)
    }
}
