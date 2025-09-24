package io.stereov.singularity.user.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.encryption.service.SensitiveCrudService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.user.core.exception.model.UserDoesNotExistException
import io.stereov.singularity.user.core.model.EncryptedUserDocument
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.SensitiveUserData
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.model.identity.HashedUserIdentity
import io.stereov.singularity.user.core.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class UserService(
    override val repository: UserRepository,
    override val encryptionService: EncryptionService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val hashService: HashService,
    override val encryptionSecretService: EncryptionSecretService,
) : SensitiveCrudService<SensitiveUserData, UserDocument, EncryptedUserDocument>() {

    override val logger = KotlinLogging.logger {}
    override val sensitiveClazz = SensitiveUserData::class.java
    override val encryptedDocumentClazz= EncryptedUserDocument::class.java

    override suspend fun doEncrypt(
        document: UserDocument,
        encryptedSensitive: Encrypted<SensitiveUserData>,
    ): EncryptedUserDocument {

        val hashedEmail = document.sensitive.email?.let { hashService.hashSearchableHmacSha256(it) }
        val hashedIdentities = document.sensitive.identities
            .map { (provider, identity) ->
                val hashedUserIdentity = HashedUserIdentity(
                    password = identity.password,
                    principalId = identity.principalId?.let { hashService.hashSearchableHmacSha256(it) },
                )
                provider to hashedUserIdentity
            }.toMap()


        return EncryptedUserDocument(
            document._id,
            hashedEmail,
            hashedIdentities,
            document.roles,
            document.groups,
            document.createdAt,
            document.lastActive,
            encryptedSensitive
        )
    }

    override suspend fun doDecrypt(
        encrypted: EncryptedUserDocument,
        decryptedSensitive: SensitiveUserData,
    ): UserDocument {
        return UserDocument(
            encrypted._id,
            encrypted.createdAt,
            encrypted.lastActive,
            encrypted.roles,
            encrypted.groups,
            decryptedSensitive
        )
    }

    /**
     * Finds a user by their email address.
     *
     * @param email The email address of the user to find.
     *
     * @return The [UserDocument] of the found user.
     *
     * @throws io.stereov.singularity.user.core.exception.model.UserDoesNotExistException If no user is found with the given email.
     */
    suspend fun findByEmail(email: String): UserDocument {
        logger.debug { "Fetching user with email $email" }

        val hashedEmail = hashService.hashSearchableHmacSha256(email)
        val encrypted =  this.repository.findByEmail(hashedEmail)
            ?: throw UserDoesNotExistException("No user found with email $email")

        return this.decrypt(encrypted)
    }

    /**
     * Finds a user by their email address, returning null if not found.
     *
     * @param email The email address of the user to find.
     *
     * @return The [UserDocument] of the found user, or null if not found.
     */
    suspend fun findByEmailOrNull(email: String): UserDocument? {
        logger.debug { "Fetching user with email $email" }

        val hashedEmail = hashService.hashSearchableHmacSha256(email)
        return this.repository.findByEmail(hashedEmail)
            ?.let { this. decrypt(it) }
    }

    /**
     * Checks if a user exists by their email address.
     *
     * @param email The email address to check.
     *
     * @return True if a user with the given email exists, false otherwise.
     */
    suspend fun existsByEmail(email: String): Boolean {
        logger.debug { "Checking if email $email already exists" }

        val hashedEmail = hashService.hashSearchableHmacSha256(email)
        return this.repository.existsByEmail(hashedEmail)
    }

    suspend fun findByIdentityOrNull(provider: String, principalId: String): UserDocument? {
        logger.debug { "Finding user by principal ID $principalId and provider $provider" }

        val hashedPrincipalId = hashService.hashSearchableHmacSha256(principalId)
        val user = repository.findByIdentity(provider, hashedPrincipalId)

        return user?.let { decrypt(it) }
    }

    suspend fun findAllByRolesContaining(role: Role): Flow<UserDocument> {
        logger.debug { "Finding all users with role $role" }

        return repository.findAllByRolesContaining(role).map { decrypt(it) }
    }

    suspend fun findAllByGroupContaining(group: String): Flow<UserDocument> {
        logger.debug { "Finding all users with group membership $group" }

        return repository.findAllByGroupsContaining(group).map { decrypt(it) }
    }

    suspend fun findAllPaginated(
        pageable: Pageable,
        email: String?,
        roles: Set<Role>?,
        groups: Set<String>?,
        createdAtBefore: Instant?,
        createdAtAfter: Instant?,
        lastActiveBefore: Instant?,
        lastActiveAfter: Instant?,
        identityKeys: Set<String>?
    ): Page<UserDocument> {
        logger.debug { "Finding ${encryptedDocumentClazz.simpleName}: page ${pageable.pageNumber}, size: ${pageable.pageSize}, sort: ${pageable.sort}" }

        val criteriaList = mutableListOf<Criteria>()

        if (email != null) {
            criteriaList.add(Criteria.where(EncryptedUserDocument::email.name)
                .isEqualTo(hashService.hashSearchableHmacSha256(email)))
        }
        if (!roles.isNullOrEmpty()) {
            criteriaList.add(Criteria.where(EncryptedUserDocument::roles.name).`in`(roles))
        }
        if (!groups.isNullOrEmpty()) {
            criteriaList.add(Criteria.where(EncryptedUserDocument::groups.name).`in`(groups))
        }
        if (createdAtAfter != null) {
            criteriaList.add(Criteria.where(EncryptedUserDocument::createdAt.name).gte(createdAtAfter))
        }
        if (createdAtBefore != null) {
            criteriaList.add(Criteria.where(EncryptedUserDocument::createdAt.name).lte(createdAtBefore))
        }
        if (lastActiveAfter != null) {
            criteriaList.add(Criteria.where(EncryptedUserDocument::lastActive.name).gte(lastActiveAfter))
        }
        if (lastActiveBefore != null) {
            criteriaList.add(Criteria.where(EncryptedUserDocument::lastActive.name).lte(lastActiveBefore))
        }

        if (!identityKeys.isNullOrEmpty()) {
            val identityCriteria = Criteria().orOperator(
                *identityKeys.map { key ->
                    Criteria.where("${EncryptedUserDocument::identities.name}.$key").exists(true)
                }.toTypedArray()
            )
            criteriaList.add(identityCriteria)
        }

        val criteria = if (criteriaList.isNotEmpty()) {
            Criteria().andOperator(*criteriaList.toTypedArray())
        } else null

        return findAllPaginated(pageable, criteria)
    }

}
