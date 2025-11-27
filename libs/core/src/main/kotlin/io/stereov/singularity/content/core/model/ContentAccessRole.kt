package io.stereov.singularity.content.core.model

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.stereov.singularity.content.core.exception.ContentAccessRoleException

/**
 * Represents the access roles for content, defining different levels of interaction.
 *
 * Roles:
 * - `VIEWER`: Grants read-only access to the content.
 * - `EDITOR`: Allows read and write access to the content but without full administrative privileges.
 * - `MAINTAINER`: Provides full access, including managerial capabilities over the content.
 */
enum class ContentAccessRole {
    /**
     * Grants read-only access to the content.
     */
    VIEWER,
    /**
     * Allows read and write access to the content but without full administrative privileges.
     */
    EDITOR,
    /**
     * Provides full access, including managerial capabilities over the content.
     */
    MAINTAINER;

    companion object {

        /**
         * Converts the given string representation into a corresponding `[ContentAccessRole]` instance.
         *
         * @param string The string representation of the desired [ContentAccessRole].
         * @return A [Result] wrapping the [ContentAccessRole] if the conversion is successful,
         *  or a [ContentAccessRoleException] if the string does not match any valid role.
         */
        fun fromString(string: String): Result<ContentAccessRole, ContentAccessRoleException> {
            return when (string.lowercase()) {
                VIEWER.toString().lowercase() -> Ok(VIEWER)
                EDITOR.toString().lowercase() -> Ok(EDITOR)
                MAINTAINER.toString().lowercase() -> Ok(MAINTAINER)
                else -> Err(ContentAccessRoleException.Invalid(string))
            }
        }
    }
}
