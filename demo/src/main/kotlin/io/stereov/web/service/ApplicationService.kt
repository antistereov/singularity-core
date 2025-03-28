package io.stereov.web.service

import io.stereov.web.model.ThisApplicationInfo
import io.stereov.web.user.model.UserDocument
import org.springframework.stereotype.Service

@Service
class ApplicationService {

    fun setApplicationInfo(user: UserDocument, info: ThisApplicationInfo) {
        val saved = user.app as? ThisApplicationInfo
        user.app = info
        user.getApplicationInfo<ThisApplicationInfo>()
    }
}
