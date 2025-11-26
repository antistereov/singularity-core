package io.stereov.singularity.principal.settings.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length

/**
 * # Change password request.
 *
 * This data class represents a request to change a user's password.
 * It contains the old password, new password, and a two-factor authentication code.
 *
 * @property oldPassword The user's current password.
 * @property newPassword The new password to be set.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class ChangePasswordRequest(
    val oldPassword: String,
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
    @field:NotBlank(message = "New password required")
    val newPassword: String,
)
