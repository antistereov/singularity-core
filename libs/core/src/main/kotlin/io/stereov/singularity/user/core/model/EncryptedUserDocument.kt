package io.stereov.singularity.user.core.model

import io.stereov.singularity.database.core.model.EncryptedSensitiveDocument
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.hash.model.SearchableHash
import io.stereov.singularity.user.core.model.identity.HashedUserIdentity
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class EncryptedUserDocument(
    @Id override val _id: ObjectId? = null,
    val email: SearchableHash?,
    val identities: Map<String, HashedUserIdentity>,
    val roles: MutableSet<Role>,
    val created: Instant = Instant.now(),
    var lastActive: Instant = Instant.now(),
    override var sensitive: Encrypted<SensitiveUserData>,
)  : EncryptedSensitiveDocument<SensitiveUserData> {

    override fun toSensitiveDocument(decrypted: SensitiveUserData, otherValues: List<Any?>) : UserDocument {
        return UserDocument(_id, created, lastActive, roles,decrypted)
    }
}
