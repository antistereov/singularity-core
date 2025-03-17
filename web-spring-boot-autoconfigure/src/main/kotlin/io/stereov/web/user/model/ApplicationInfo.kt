package io.stereov.web.user.model

import io.stereov.web.user.dto.ApplicationInfoDto

interface ApplicationInfo {

    fun toDto(): ApplicationInfoDto
}
