package io.stereov.singularity.secrets.core.properties

/**
 * Specifies the implementation type for the secret store.
 *
 * This enum is used to define the configuration of how secrets are managed
 * within the system. The available implementations are:
 *
 * - `VAULT`: Represents an external Vault-based secret store implementation.
 * - `LOCAL`: Represents a local, minimalistic secret store implementation using a H2 database.
 *
 * The `SecretStoreImplementation` is commonly used in configuration properties
 * to determine the desired secret management approach for the application.
 */
enum class SecretStoreImplementation {
    /**
     * Represents an external Vault-based secret store implementation.
     *
     * This option uses HashiCorp Vault to securely store and manage secrets.
     * It is typically configured by setting the `singularity.secrets.store` property
     * to `VAULT` in the application configuration.
     *
     * Additional configuration properties may include the Vault host, port,
     * and authentication token, which allow the application to connect to
     * the external Vault instance.
     *
     * This implementation is suitable for systems requiring a centralized
     * and secure solution for secret management at scale.
     */
    VAULT,

    /**
     * Represents a local, minimalistic secret store implementation using an H2 database.
     *
     * This implementation is designed for systems where a lightweight
     * and self-contained secret management solution is preferred.
     * It uses a local database to store and manage secrets securely.
     *
     * By default, this implementation is set when configuring the `singularity.secrets.store` property
     * to `LOCAL` in the application configuration.
     *
     * This approach is suitable for smaller-scale systems or environments where an
     * external secret management system, such as Vault, is not necessary.
     */
    LOCAL
}
