package io.stereov.singularity.user.model

import io.stereov.singularity.database.model.EncryptedSensitiveDocument
import io.stereov.singularity.encryption.model.Encrypted
import io.stereov.singularity.hash.model.SearchableHash
import io.stereov.singularity.hash.model.SecureHash
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class EncryptedUserDocument(
    @Id override val _id: ObjectId? = null,
    val email: SearchableHash,
    var password: SecureHash,
    val created: Instant = Instant.now(),
    var lastActive: Instant = Instant.now(),
    override var sensitive: Encrypted<SensitiveUserData>,
)  : EncryptedSensitiveDocument<SensitiveUserData> {

    override fun toSensitiveDocument(decrypted: SensitiveUserData, otherValues: List<Any>) : UserDocument {
        return UserDocument(_id, password, created, lastActive, decrypted)
    }
}
