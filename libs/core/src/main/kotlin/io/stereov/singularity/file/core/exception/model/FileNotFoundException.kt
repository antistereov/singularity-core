package io.stereov.singularity.file.core.exception.model

import io.stereov.singularity.file.core.exception.FileException
import java.io.File

class FileNotFoundException(file: File?, msg: String? = null, cause: Throwable? = null) : FileException(
    msg = if (msg != null) "$msg: No file found: ${file?.absolutePath}" else "No file found: ${file?.absolutePath}",
    cause)
