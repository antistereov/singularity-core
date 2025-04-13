package io.stereov.web.global.service.file.exception

import io.stereov.web.global.exception.BaseWebException
import io.stereov.web.global.service.file.util.toResource
import java.io.File

open class FileException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause) {

    init {
        val file = File("./README.md")
        file.toResource()
    }
}
