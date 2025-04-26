package io.stereov.singularity.user.model

import io.stereov.singularity.user.dto.ApplicationInfoDto

/**
 * # ApplicationInfo interface.
 *
 * This interface defines a contract for classes that provide application information.
 * It includes a method to convert the application information to a DTO (Data Transfer Object).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
interface ApplicationInfo {

    fun toDto(): ApplicationInfoDto
}
