package io.stereov.singularity.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * # Authentication properties.
 *
 * This class is responsible for holding the authentication properties
 * and is annotated with [ConfigurationProperties]
 * to bind the properties from the application configuration file.
 *
 * It is prefixed with `baseline.auth` in the configuration file.
 *
 * @property publicPaths List of public paths that do not require authentication.
 * @property userPaths List of user paths that require user authentication.
 * @property adminPaths List of admin paths that require admin authentication.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@ConfigurationProperties(prefix = "baseline.auth")
data class AuthProperties(
    val publicPaths: List<String> = emptyList(),
    val userPaths: List<String> = emptyList(),
    val adminPaths: List<String> = emptyList(),
)
