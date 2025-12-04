# Cache

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

**Caching** allows storing resources that are accessed often to be stored in your server's memory.
This saves time and bandwidth.
*Singularity* uses [Redis](https://redis.io/) to cache data.

## Configuration

### Quickstart

If you just want to test *Singularity*, 
you can use the prebuilt and preconfigured [`docker-compose.yaml`](https://github.com/antistereov/singularity/blob/548bcba3ce6d0c1bdbacc2861c2726b1dc1d7991/libs/core/infrastructure/docker/docker-compose.yaml).
Copy it into your project's root directory and run:

```shell
docker compose up -d
```

This will start a preconfigured instance of Redis.
It also includes a preconfigured instance of MongoDB which is the database *Singularity* uses.
You can learn more about the database [here](database/connection.md).

:::note
You can learn more about Docker and how to use it [here](https://docs.docker.com/).
:::

### Custom

For production and advanced use-cases you should provide your own instance of *Redis*.

You can configure the connection in your `application.yaml` like this:

```yaml
spring:
  data:
    redis:
      host: <hostname>
      port: <port>
      password: <password> # if enabled
      database: <database> # just use 0 by default
      timeout: 5000ms
      ssl:
        enabled: false # or set to true if you need SSL
```

:::note
*Singularity* uses [Spring Data Redis](https://docs.spring.io/spring-data/redis/reference/index.html) to implement *Redis*.
:::

## Core Components

### `CacheService`

| Method Name            | Description                                                                                                                                  | Signature                                                                                             |
|:-----------------------|:---------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------------|
| `put`                  | Saves a key-value pair in Redis with an optional expiration time.                                                                            | `suspend fun <T: Any> put(key: String, value: T, expiresIn: Long? = null): Result<T, CacheException>` |
| `exists`               | Checks if a value exists in Redis for the given key.                                                                                         | `suspend fun exists(key: String): Result<Boolean, CacheException.Operation>`                          |
| `get`                  | Retrieves a value associated with the given key from the Redis cache and deserializes it to type `T`.                                        | `final suspend inline fun <reified T: Any> get(key: String): Result<T, CacheException>`               |
| `delete`               | Deletes the specified keys from the Redis cache.                                                                                             | `suspend fun delete(vararg keys: String): Result<Long, CacheException.Operation>`                     |
| `deleteAll`            | Deletes all keys from the Redis cache, optionally matching a specified pattern. If no pattern is provided, the entire cache is flushed.      | `suspend fun deleteAll(pattern: String? = null): Result<Unit, CacheException.Operation>`              |
| `startCooldown`        | Initiates a cooldown period by storing a key-value pair, ensuring the operation is atomic and only occurs if the key does not already exist. | `suspend fun startCooldown(key: String, seconds: Long): Result<Boolean, CacheException.Operation>`    |
| `getRemainingCooldown` | Retrieves the remaining cooldown duration for the specified key.                                                                             | `suspend fun getRemainingCooldown(key: String): Result<Duration, CacheException.Operation>`           |

Let me know if you need any of these methods explained in more detail, or if you'd like to update any other part of your documentation.

## Usage

*Singularity* provides a [CacheService](https://github.com/antistereov/singularity/blob/main/libs/core/src/main/kotlin/io/stereov/singularity/cache/service/CacheService.kt)
that helps you with storing, updating, reading and deleting data from the cache.

The `CacheService` now returns a [Result](https://github.com/michaelbull/kotlin-result) type, which explicitly wraps either the successful value (`Ok`) or a domain-specific error (`Err`), 
typically a `CacheException`. This requires handling the result in your calling code.

### Example

This example shows you how to improve performance by caching objects of the `CoolStuff` class
when accessed from the database, while handling the new `Result` return types.

```kotlin
@Service
class CoolService(
    // The CacheService provided by Singularity
    private val cacheService: CacheService,
    // Your service that allows you to access the database directly
    private val coolStuffDatabaseService: CoolStuffDatabaseService
) {

    // The prefix unique to CoolStuff
    // It is highly recommended to use a custom prefix for every type of data you want to store.
    val prefix = "cool-stuff"

    /**
     * Redis is a key-value store.
     */
    private fun getRedisKey(id: String): String {

        // Create a key using the prefix and an ID delimited by a colon (:)
        return "$prefix:$id"
    }

    /**
     * A method that retrieves cool stuff.
     * It checks if the CoolStuff with given ID is stored in cache.
     *
     * If yes, it returns it from the cache directly.
     * If no, it fetches it from the database and stores it in the cache automatically.
     *
     * This method now uses 'coroutineBinding' to handle the Results returned by the CacheService.
     * This allows us to use 'bind()' to unwrap the success value or automatically return the error.
     */
    suspend fun getCoolStuff(id: String): Result<CoolStuff, Exception> = coroutineBinding {
        // Create the Redis key for this object
        val redisKey = getRedisKey(id)

        // Check if a value with this key already exists.
        // We use .bind() to unwrap the Boolean result or propagate a CacheException.Operation error.
        val coolStuffIsCached = cacheService.exists(redisKey).bind()

        if (coolStuffIsCached) {
            // If it is already cached, we can fetch the value and return it.
            // get<T>() returns a Result<T, CacheException>. We use .bind() again.
            // If the key is found but deserialization fails, the CacheException.ObjectMapper error
            // will be immediately returned from the coroutineBinding block.
            cacheService.get<CoolStuff>(redisKey).bind()
        } else {
            // Retrieve the object from the database
            val coolStuffFromDatabase = coolStuffDatabaseService.getById(id)

            // Store it in the cache.
            // put() returns a Result<T, CacheException>. We use .bind() here, though in this context,
            // we primarily care that it completes successfully or propagates an error.
            cacheService.put(redisKey, coolStuffFromDatabase).bind()
        }
    }

    /**
     * You can invalidate the whole CoolStuff.
     * It returns a Result<Unit, CacheException.Operation>.
     */
    suspend fun invalidateCache(): Result<Unit, CacheException.Operation> {
        val pattern = "$prefix:"

        /**
         * The deleteAll method deletes all keys with a given prefix.
         */
        return cacheService.deleteAll(pattern)
    }

    /**
     * You can delete a single or multiple keys.
     * It returns a Result<Long, CacheException.Operation>.
     */
    suspend fun deleteCached(vararg ids: String): Result<Long, CacheException.Operation> {
        val keys = ids.map { id -> "$prefix:$id" }

        // delete() returns a Result<Long, CacheException.Operation>
        return cacheService.delete(*keys.toTypedArray())
    }
}
```
