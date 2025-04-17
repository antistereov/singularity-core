package io.stereov.web.global.service.secrets.component

import com.bitwarden.sdk.BitwardenClient
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.config.Constants
import io.stereov.web.global.service.secrets.model.Secret
import io.stereov.web.properties.secrets.BitwardenKeyManagerProperties
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

@Component
@ConditionalOnProperty(prefix = "baseline.secrets", value = ["key-manager"], havingValue = "bitwarden", matchIfMissing = false)
class BitwardenKeyManager(
    private val bitwardenClient: BitwardenClient,
    private val properties: BitwardenKeyManagerProperties,
) : KeyManager {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private var encryptionSecret: Secret? = null
    private var jwtSecret: Secret? = null

    private val loadedKeys = mutableSetOf<Secret>()

    @PostConstruct
    fun init() {
        getJwtSecret()
        getEncryptionSecret()
    }

    override fun getSecretById(id: UUID): Secret {
        logger.debug { "Getting secret by ID $id" }

        val cachedSecret = this.loadedKeys.firstOrNull { it.id == id }
        if (cachedSecret != null) return cachedSecret

        this.bitwardenClient.secrets().sync(properties.organizationId, OffsetDateTime.now())
        val secretResponse = bitwardenClient.secrets().get(id)
        val secret = Secret(secretResponse.id, secretResponse.value)

        this.loadedKeys.add(secret)
        return secret
    }

    override fun getEncryptionSecret(): Secret {
        this.logger.debug { "Getting current encryption secret" }

        return this.encryptionSecret
            ?: this.loadCurrentEncryptionSecret()
    }

    fun loadCurrentEncryptionSecret(): Secret {
        logger.debug { "Loading current encryption secret" }

        val secret = this.getCurrentSecretByKey(Constants.CURRENT_ENCRYPTION_SECRET)
            ?: updateEncryptionSecret()

        this.encryptionSecret = secret
        this.loadedKeys.add(secret)

        return secret
    }

    override fun updateEncryptionSecret(): Secret {
        logger.info { "Updating current encryption secret "}

        val key = "encryption-secret-${Instant.now()}"
        val secret = generateKey()
        val note = "Encryption secret generated on ${Instant.now()}"

        val newSecret = create(key, secret, note)
        this.encryptionSecret = newSecret

        this.createOrUpdateKey(Constants.CURRENT_ENCRYPTION_SECRET, newSecret.id.toString(), note)

        return newSecret
    }

    override fun getJwtSecret(): Secret {
        this.logger.debug { "Getting JWT secret" }

        val currentSecret = this.jwtSecret
        if (currentSecret != null) return currentSecret

        val secret = getSecretByKey(Constants.CURRENT_JWT_SECRET)
            ?: updateJwtSecret()

        this.jwtSecret = secret

        return secret
    }

    override fun updateJwtSecret(): Secret {
        logger.info { "Updating current jwt secret "}

        val key = Constants.CURRENT_JWT_SECRET
        val secret = generateKey(algorithm = "HmacSHA256")
        val note = "Jwt secret generated on ${Instant.now()}"

        val newSecret = createOrUpdateKey(key, secret, note)
        this.encryptionSecret = newSecret

        return newSecret
    }

    fun getSecretByKey(key: String): Secret? {
        this.logger.debug { "Getting secret by key $key" }

        val res = this.bitwardenClient.secrets().list(properties.organizationId).data
            .firstOrNull { secret ->
                secret.key == key
            }

        return res?.let { getSecretById(res.id) }
    }

    fun getCurrentSecretByKey(key: String): Secret? {
        this.logger.debug { "Getting current secret by key $key" }

        return this.getSecretByKey(key)?.let {
            this.getSecretById(it.id)
        }
    }

    fun create(key: String, value: String, note: String): Secret {
        this.logger.debug { "Creating secret with key $key" }

        val secretResponse = bitwardenClient.secrets().create(properties.organizationId, key, value, note, arrayOf(properties.projectId))

        val id = secretResponse.id

        return Secret(id, value)
    }

    fun createOrUpdateKey(key: String, value: String, note: String): Secret {
        this.logger.debug { "Creating or updating key $key" }

        val existingSecret = getSecretByKey(key) ?: return this.create(key, value, note)

        return update(existingSecret.id, key, value, note)
    }

    fun update(id: UUID, key: String, value: String, note: String): Secret {
        val res = this.bitwardenClient.secrets().update(properties.organizationId, id, key, value, note, arrayOf(properties.projectId))

        return Secret(res.id, res.value)
    }
}
