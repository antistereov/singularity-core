package io.stereov.web.global.service.secrets.component

import io.stereov.web.global.service.secrets.model.Secret
import java.util.*

interface KeyManager {

    fun getSecretById(id: UUID): Secret
    fun getSecretByKey(key: String): Secret?

    fun create(key: String, value: String, note: String): Secret
    fun createOrUpdateKey(key: String, value: String, note: String): Secret
    fun update(id: UUID, key: String, value: String, note: String): Secret
}
