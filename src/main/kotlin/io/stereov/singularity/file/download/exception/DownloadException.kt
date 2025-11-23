package io.stereov.singularity.file.download.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class DownloadException(
    msg: String,
    code: String,
    status: HttpStatus,
    cause: Throwable? = null
) : SingularityException(msg, code, status, cause) {

    class FileNotFound(url: String, cause: Throwable? = null) : DownloadException(
        msg = "File not found: $url",
        code = CODE,
        status = STATUS,
        cause = cause
    ) {
        companion object {
            const val CODE = "DOWNLOAD_FILE_NOT_FOUND"
            val STATUS = HttpStatus.NOT_FOUND
        }
    }

    class BadRequest(url: String, cause: Throwable? = null) : DownloadException(
        msg = "Bad request for: $url",
        CODE,
        STATUS,
        cause
    ) {
        companion object {
            const val CODE = "DOWNLOAD_BAD_REQUEST"
            val STATUS = HttpStatus.BAD_REQUEST
        }
    }

    class Unauthorized(url: String, cause: Throwable? = null) : DownloadException(
        msg = "Unauthorized access to: $url",
        code = CODE,
        status = STATUS,
        cause = cause
    ) {
        companion object {
            const val CODE = "DOWNLOAD_UNAUTHORIZED"
            val STATUS = HttpStatus.UNAUTHORIZED
        }
    }

    class Forbidden(url: String, cause: Throwable? = null) : DownloadException(
        msg = "Forbidden access to: $url",
        code = CODE,
        status = STATUS,
        cause = cause
    ) {
        companion object {
            const val CODE = "DOWNLOAD_FORBIDDEN"
            val STATUS = HttpStatus.UNAUTHORIZED
        }
    }

    class HttpError(url: String, cause: Throwable? = null) : DownloadException(
        msg = "HTTP error for $url",
        code = CODE,
        status = STATUS,
        cause = cause
    ) {
        companion object {
            const val CODE = "DOWNLOAD_HTTP_ERROR"
            val STATUS = HttpStatus.BAD_GATEWAY
        }
    }

    class NetworkError(url: String, cause: Throwable? = null) : DownloadException(
        msg = "Network error while downloading $url",
        code = CODE,
        status = STATUS,
        cause = cause
    ) {
        companion object {
            const val CODE = "DOWNLOAD_NETWORK_ERROR"
            val STATUS = HttpStatus.SERVICE_UNAVAILABLE
        }
    }

    class FileTooLarge(url: String, cause: Throwable? = null) : DownloadException(
        msg = "File too large: $url",
        CODE,
        STATUS,
        cause
    ) {
        companion object {
            const val CODE = "DOWNLOAD_FILE_TOO_LARGE"
            val STATUS = HttpStatus.PAYLOAD_TOO_LARGE
        }
    }

    class Timeout(url: String, cause: Throwable? = null) : DownloadException(
        msg = "Timeout while downloading $url",
        code = CODE,
        status = STATUS,
        cause = cause
    ) {
        companion object {
            const val CODE = "DOWNLOAD_TIMEOUT"
            val STATUS = HttpStatus.GATEWAY_TIMEOUT
        }
    }

    class InternalError(url: String, cause: Throwable? = null) : DownloadException("Internal error for $url", CODE, STATUS, cause) {
        companion object {
            const val CODE = "DOWNLOAD_INTERNAL_ERROR"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    class Unknown(url: String, cause: Throwable? = null) : DownloadException("Unknown error for $url", CODE, STATUS, cause) {
        companion object {
            const val CODE = "DOWNLOAD_UNKNOWN_ERROR"
            val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
        }
    }
}
