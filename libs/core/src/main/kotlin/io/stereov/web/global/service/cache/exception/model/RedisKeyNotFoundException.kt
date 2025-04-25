package io.stereov.web.global.service.cache.exception.model

import io.stereov.web.global.service.cache.exception.RedisException

/**
 * # Exception thrown when a key is not found in Redis.
 *
 * This exception is used to indicate that the specified key does not exist in Redis.
 * It extends the [RedisException] class.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class RedisKeyNotFoundException(key: String) : RedisException("Key not found in Redis: $key")
