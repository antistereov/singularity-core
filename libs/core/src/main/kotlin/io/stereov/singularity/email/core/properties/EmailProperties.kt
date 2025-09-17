package io.stereov.singularity.email.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.email")
data class EmailProperties(
    val enable: Boolean = false,
    val host: String = "host.com",
    val port: Int = 0,
    val email: String = "email@host.com",
    val username: String = "email@host.com",
    val password: String = "password",
    val transportProtocol: String = "smtp",
    val smtpAuth: Boolean = true,
    val smtpStarttls: Boolean = true,
    val debug: Boolean = false,
    val sendCooldown: Long = 60,
)
