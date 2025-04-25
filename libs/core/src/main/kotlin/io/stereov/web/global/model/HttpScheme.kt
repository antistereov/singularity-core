package io.stereov.web.global.model

enum class HttpScheme {
    HTTP, HTTPS;

    override fun toString(): String {
        return when (this) {
            HTTP -> "http://"
            HTTPS -> "https://"
        }
    }
}
