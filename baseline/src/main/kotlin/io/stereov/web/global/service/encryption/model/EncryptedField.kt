package io.stereov.web.global.service.encryption.model

import java.util.*

data class EncryptedField(
    val keyId: UUID,
    val data: String,
)
