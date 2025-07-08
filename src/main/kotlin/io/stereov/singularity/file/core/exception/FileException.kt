package io.stereov.singularity.file.core.exception

import io.stereov.singularity.file.core.util.toResource
import io.stereov.singularity.global.exception.BaseWebException
import java.io.File

open class FileException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause) {

    init {
        val file = File("./README.md")
        file.toResource()
    }
}
