package io.stereov.singularity.global.util

import io.stereov.singularity.global.exception.SingularityException
import io.swagger.v3.oas.annotations.responses.ApiResponse

class OpenApiDocUtil {

    companion object {
        @JvmStatic
        fun generateErrorDocumentation(possibleErrors: Collection<SingularityException>): Array<ApiResponse> {
            return TODO()
        }
    }
}
