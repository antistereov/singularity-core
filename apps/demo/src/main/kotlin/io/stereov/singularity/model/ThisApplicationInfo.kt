package io.stereov.singularity.model

import io.stereov.singularity.dto.ThisAppDto
import io.stereov.singularity.user.dto.ApplicationInfoDto
import io.stereov.singularity.user.model.ApplicationInfo

data class ThisApplicationInfo(
    val favoriteColor: String
) : ApplicationInfo {

    override fun toDto(): ApplicationInfoDto {
        return ThisAppDto(favoriteColor)
    }
}
