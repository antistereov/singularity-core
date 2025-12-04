---
sidebar_position: 1
---

# Basics

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

Managing secrets like API keys, database credentials, and encryption keys securely is a critical security practice. 
Secrets should never be hardcoded in your application's source code.

*Singularity* provides a robust, pluggable secret store that handles the management, rotation, and retrieval of secrets.

## Core Concepts

#### `Secret`

This is the fundamental data model for a secret. 
It stores a unique identifier (`id`), a developer-friendly key (`key`),
the secret value itself (`value`), and a timestamp for when it was created (`createdAt`).

#### `SecretStore`

This interface abstracts the underlying storage mechanism for secrets, 
allowing for different implementations like HashiCorp Vault or a local file system. 
The `SecretStore` also includes built-in caching to improve performance by reducing 
the number of calls to the external secret manager.  
Secrets loaded from the store are automatically cached with an expiration time.

#### `SecretService`

This is the core class for managing a single, specific secret. 
It handles the logic for retrieving the current secret and for key rotation. 
It relies on a `SecretStore` to persist the secrets.

## Usage

To use the secret store, you must define a class that extends `SecretService`. 
This class will be responsible for managing a single type of secret.

### Step 1: Define a `SecretService`

By extending the abstract `SecretService`, 
you define a new type of secret that the framework's key rotation service will recognize and manage automatically. 
You must provide the `SecretStore` and `AppProperties` dependencies, along with a unique key for your secret.

```kotlin
@Service
class MySecretService(
    secretStore: SecretStore,
    appProperties: AppProperties
) : SecretService(
    secretStore = secretStore,
    key = "my-secret-key",
    algorithm = "AES",
    appProperties = appProperties
) {
    override val logger = KotlinLogging.logger {}
}
```

### Step 2: Use the Secret

Once defined, you can inject your custom `SecretService` into any other service or component to retrieve the current secret's value.

```kotlin
@Service
class MyOtherService(
    private val mySecretService: MySecretService
) {
    suspend fun doSomethingSecure(): Result<SomethingSecret, SecretStoreException> = coroutineBinding {
        val secret = mySecretService.getCurrentSecret().bind().value
        
        // Use the secret value for your secure operation
        SomethingSecret.fromSecret(secret)
    }
}
```

## Secret Key Rotation

To mitigate the risk of a compromised key, the secret keys will be rotated periodically.
Key rotation is automatically scheduled to run via a cron job.
You can learn more about the configuration in the [next section](#configuration).

You can also manually trigger key rotation through the following endpoints:

* [`POST /api/security/secrets/rotate-keys`](../../api/rotate-secret-keys.api.mdx)
  Triggers an immediate secret rotation for all defined services. This operation is asynchronous.
* [`GET /api/security/secrets/rotate-keys/status`](../../api/get-secret-key-rotation-status.api.mdx)
  Returns the current status of the secret rotation process. 
  The response indicates whether a rotation is ongoing and returns the timestamp of the last successful rotation.\


## Configuration

| Property                              | Type                   | Description                                                                                                                                                                                                                                       | Default value   |
|---------------------------------------|------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|
| singularity.secrets.store             | `LOCAL` or `VAULT`     | *Singularity* currently provides two different implementations for secret stores: [`LOCAL`](local.md) and [`VAULT`](vault.md). The local secret store is preconfigured. For large-scale application it is recommended to use [`VAULT`](vault.md). | `LOCAL`         |
| singularity.secrets.key-rotation-cron | `String` of a cron job | The scheduled rate when key rotation should be performed. Default is every three months on the first day of the month at 04:00:00 am.                                                                                                             | `0 0 4 1 */3 *` |
| singularity.secrets.cache-expiration  | `Long`                 | Secrets are cached for a specific amount of time. This value configures after how many seconds they will expire.                                                                                                                                  | `900000`        |
