package io.stereov.web.user.model

import io.stereov.web.global.service.encryption.model.Encrypted
import io.stereov.web.global.service.encryption.model.EncryptedSensitiveDocument
import io.stereov.web.global.service.hash.model.HashedField
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class EncryptedUserDocument(
    @Id val _id: String? = null,
    val email: HashedField,
    var password: HashedField,
    val created: Instant = Instant.now(),
    var lastActive: Instant = Instant.now(),
    var app: ApplicationInfo? = null,
    var sensitiveUserData: Encrypted<SensitiveUserData>,
)  : EncryptedSensitiveDocument<SensitiveUserData>(sensitiveUserData) {

    override fun toSensitiveDocument(decrypted: SensitiveUserData, otherValues: List<Any>) : UserDocument {
        return UserDocument(_id, password, created, lastActive, app, decrypted)
    }
}
