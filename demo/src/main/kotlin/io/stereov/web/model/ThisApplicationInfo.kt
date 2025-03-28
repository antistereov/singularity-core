package io.stereov.web.model

import io.stereov.web.dto.ThisAppDto
import io.stereov.web.user.dto.ApplicationInfoDto
import io.stereov.web.user.model.ApplicationInfo

data class ThisApplicationInfo(
    val favoriteColor: String
) : ApplicationInfo {

    override fun toDto(): ApplicationInfoDto {
        return ThisAppDto(favoriteColor)
    }
}
