package io.stereov.web.properties.secrets

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("baseline.secrets")
data class KeyManagerProperties(
    val keyManager: KeyManagerImplementation = KeyManagerImplementation.Bitwarden
) {

}
