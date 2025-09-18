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
Just copy it into your project's root directory and run:

```shell
docker compose up -d
```

This will start a preconfigured instance of Redis.
It also includes a preconfigured instance of MongoDB which is the database *Singularity* uses.
You can learn more about the database [here](./database/configuration.md).

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

## Usage

*Singularity* provides a [CacheService](https://github.com/antistereov/singularity/blob/main/libs/core/src/main/kotlin/io/stereov/singularity/cache/service/CacheService.kt)
that helps you with storing, updating, reading and deleting data from the cache.

### Example

This example shows you how to improve performance by caching objects of the `CoolStuff` class
when accessed from the database.

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
     */
    suspend fun getCoolStuff(id: String): CoolStuff {
        // Create the Redis key for this object
        val redisKey = getRedisKey(id)

        // Check if a value with this key already exists
        val coolStuffIsCached = cacheService.exists(redisKey)

        if (coolStuffIsCached) {
            // If it is already cached, we can fetch the value and return it
            // You need to provide a Type Generic so the service knows what type the value has.
            return cacheService.get<CoolStuff>(redisKey)
        }

        // Retrieve the object from the database
        val coolStuffFromDatabase = coolStuffDatabaseService.getById(id)

        // Store it in the cache
        // Note that if data existed on this key already, it will be replaced.
        cacheService.put(redisKey, coolStuffFromDatabase)

        // Return the CoolStuff you retrieved from the database
        return coolStuffFromDatabase
    }

    /**
     * You can invalidate the whole CoolStuff.
     */
    suspend fun invalidateCache() {
        val pattern = "$prefix:"

        /**
         * The deleteAll method deletes all keys with a given prefix.
         * If you use the prefix associated to the type,
         * it will automatically delete all entries of this type.
         * 
         * If you don't provide a pattern, it will flush the whole cache.
         */
        cacheService.deleteAll(pattern)
    }

    /**
     * You can delete a single or multiple keys.
     */
    suspend fun deleteCached(vararg ids: String) {
        val keys = ids.map { id -> "$prefix:$id" }
        
        cacheService.delete(*keys.toTypedArray())
    }
}


```