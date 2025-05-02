package io.stereov.singularity.service

import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.model.ThisApplicationInfo
import org.springframework.stereotype.Service

@Service
class ApplicationService {

    fun setApplicationInfo(user: UserDocument, info: ThisApplicationInfo) {
        val saved = user.app as? ThisApplicationInfo
        user.app = info
        user.getApplicationInfo<ThisApplicationInfo>()
    }
}
