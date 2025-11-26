package io.stereov.singularity.principal.core.dto.request

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length

data class ConvertToUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Schema(description = "The user's unique email address.", example = "jane.doe@example.com")
    val email: String,

    @field:Length(min = 8, message = "The password must be at least 8 characters long.")
    @field:Pattern(
        regexp = """^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-={}\[\]|\\:;'"<,>.?/]).{8,}""",
        message = "The password must be at least 8 characters long and include at least one uppercase letter, " +
                "one lowercase letter, one number, and one special character (!@#$%^&*()_+={}[]|\\:;'\"<>,.?/)."
    )
    @field:Schema(description = "The user's chosen password. " +
            "It must be at least 8 characters long and include at least one uppercase letter, " +
            "one lowercase letter, one number, and one special character (!@#$%^&*()_+={}[]|\\:;'\"<>,.?/).",
        example = "S3cur3P@ssw0rd!"
    )
    val password: String,
    val session: SessionInfoRequest?
)
