package io.stereov.singularity.global.service.file.exception

import io.stereov.singularity.global.exception.BaseWebException
import io.stereov.singularity.global.service.file.util.toResource
import java.io.File

open class FileException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause) {

    init {
        val file = File("./README.md")
        file.toResource()
    }
}
