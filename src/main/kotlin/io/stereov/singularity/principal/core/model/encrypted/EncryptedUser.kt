package io.stereov.singularity.principal.core.model.encrypted

import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.hash.model.SearchableHash
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.identity.HashedUserIdentities
import io.stereov.singularity.principal.core.model.sensitve.SensitiveUserData
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * Represents an encrypted [User] entity stored as a principal in the "principals" collection.
 *
 * This class encapsulates user-specific information such as hashed email, identities,
 * roles, groups, and sensitive data in an encrypted format. It extends [EncryptedPrincipal]
 * to provide structure and behavior for user-related data management in a secure manner.
 *
 * @constructor Creates a new instance of [EncryptedUser].
 *
 * @property _id The unique identifier for the user, represented as an optional [ObjectId].
 * @property email The hashed email of the user, stored as a [SearchableHash] for secure search.
 * @property identities A collection of hashed user identities represented by [HashedUserIdentities],
 * containing both password-based and provider-based identities.
 * @property roles The set of [Role.User.USER] assigned to the user, defining their permissions and access level.
 * @property groups The set of group IDs the user belongs to.
 * @property createdAt The timestamp indicating when the user entity was created. Defaults to the current instant.
 * @property lastActive The timestamp of the user's last activity. Defaults to the current instant.
 * @property sensitive An instance of [Encrypted] containing encrypted sensitive user data of type [SensitiveUserData].
 */
@Document(collection = "principals")
data class EncryptedUser(
    @Id override val _id: ObjectId? = null,
    val email: SearchableHash,
    val identities: HashedUserIdentities,
    override val roles: Set<Role.User>,
    override val groups: Set<String>,
    override val createdAt: Instant = Instant.now(),
    override var lastActive: Instant = Instant.now(),
    override var sensitive: Encrypted<SensitiveUserData>,
)  : EncryptedPrincipal<Role.User, SensitiveUserData>
