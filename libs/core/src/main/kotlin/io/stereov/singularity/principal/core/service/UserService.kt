package io.stereov.singularity.principal.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.core.util.CriteriaBuilder
import io.stereov.singularity.database.encryption.exception.EncryptionException
import io.stereov.singularity.database.encryption.exception.FindAllEncryptedDocumentsPaginatedException
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionSecretService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.encryption.service.SensitiveCrudService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.principal.core.exception.ExistsUserByEmailException
import io.stereov.singularity.principal.core.exception.FindUserByEmailException
import io.stereov.singularity.principal.core.exception.FindUserByProviderIdentityException
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.model.encrypted.EncryptedUser
import io.stereov.singularity.principal.core.model.identity.HashedUserIdentities
import io.stereov.singularity.principal.core.model.identity.HashedUserIdentity
import io.stereov.singularity.principal.core.model.sensitve.SensitiveUserData
import io.stereov.singularity.principal.core.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service for managing and retrieving [User] data, with sensitive data encryption/decryption and additional
 * search capabilities based on various criteria.
 */
@Service
class UserService(
    override val repository: UserRepository,
    override val encryptionService: EncryptionService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val hashService: HashService,
    override val encryptionSecretService: EncryptionSecretService,
) : SensitiveCrudService<SensitiveUserData, User, EncryptedUser>() {

    override val logger = KotlinLogging.logger {}
    override val sensitiveClazz = SensitiveUserData::class.java
    override val encryptedDocumentClazz= EncryptedUser::class.java

    override suspend fun doEncrypt(
        document: User,
        encryptedSensitive: Encrypted<SensitiveUserData>,
    ): Result<EncryptedUser, EncryptionException> = coroutineBinding {

        val hashedEmail = hashService.hashSearchableHmacSha256(document.email)
            .mapError { ex -> EncryptionException.ObjectMapping("Failed to encrypt user document because no searchable hash of email could be generated: ${ex.message}", ex) }
            .bind()

        val hashedProviders = document.sensitive.identities.providers
            .map { (provider, identity) ->
                val hashedUserIdentities = HashedUserIdentity.Provider(
                    principalId = identity.principalId.let { hashService.hashSearchableHmacSha256(it) }
                        .mapError { ex -> EncryptionException.ObjectMapping("Failed to encrypt user document because no searchable hash of principal ID could be generated for provider $provider: ${ex.message}", ex) }
                        .bind(),
                )
                provider to hashedUserIdentities
            }.toMap()
        val hashedPassword = document.sensitive.identities.password?.let {  HashedUserIdentity.Password(it.password) }

        EncryptedUser(
            document._id,
            hashedEmail,
            HashedUserIdentities(hashedPassword, hashedProviders),
            document.roles,
            document.groups,
            document.createdAt,
            document.lastActive,
            encryptedSensitive
        )
    }

    override suspend fun doDecrypt(
        encrypted: EncryptedUser,
        decryptedSensitive: SensitiveUserData,
    ): Result<User, EncryptionException> {
        return Ok(
            User(
            encrypted._id,
            encrypted.createdAt,
            encrypted.lastActive,
            encrypted.roles.contains(Role.User.ADMIN),
            encrypted.groups.toMutableSet(),
            decryptedSensitive
        ))
    }

    /**
     * Finds a user by their email address.
     *
     * @param email The email address of the user to search for.
     * @return A [Result] containing the [User] if found, or a [FindUserByEmailException]
     * describing the failure if an error occurs.
     */
    suspend fun findByEmail(email: String): Result<User, FindUserByEmailException> = coroutineBinding {
        logger.debug { "Fetching user with email $email" }

        val hashedEmail = hashService.hashSearchableHmacSha256(email)
            .mapError { ex -> FindUserByEmailException.Hash("Failed to hash email $email: ${ex.message}", ex) }
            .bind()
        val encrypted =  runSuspendCatching { repository.findByEmail(hashedEmail) }
            .mapError { ex -> FindUserByEmailException.Database("Failed to find user by email: ${ex.message}", ex) }
            .flatMap { it.toResultOr { FindUserByEmailException.UserNotFound("No user found with email $email") } }
            .bind()

        decrypt(encrypted)
            .mapError { ex -> FindUserByEmailException.Encryption("Failed to decrypt user data: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Checks whether a user with the specified email already exists in the system.
     *
     * @param email The email address to check for existence.
     * @return A [Result] containing true if the email exists, false if it does not exist,
     * or an [ExistsUserByEmailException] if an error occurs during the process.
     */
    suspend fun existsByEmail(email: String): Result<Boolean, ExistsUserByEmailException> = coroutineBinding {
        logger.debug { "Checking if email $email already exists" }

        val hashedEmail = hashService.hashSearchableHmacSha256(email)
            .mapError { ex -> ExistsUserByEmailException.Hash("Failed to hash email $email: ${ex.message}", ex) }
            .bind()
        
        runSuspendCatching { repository.existsByEmail(hashedEmail) }
            .mapError { ex -> ExistsUserByEmailException.Database("Failed to check existence of user document with email $email: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Finds a user by their provider and principal ID.
     *
     * @param provider The name of the identity provider (e.g., Google, Facebook).
     * @param principalId The principal ID associated with the user in the specified provider.
     * @return A [Result] wrapping the [User] object if found and successfully processed,
     *  or an instance of [FindUserByProviderIdentityException] if an error occurs.
     */
    @Suppress("UNUSED")
    suspend fun findByProviderIdentity(
        provider: String, 
        principalId: String
    ): Result<User, FindUserByProviderIdentityException> = coroutineBinding {
        logger.debug { "Finding user by principal ID $principalId and provider $provider" }

        val hashedPrincipalId = hashService.hashSearchableHmacSha256(principalId)
            .mapError { ex -> FindUserByProviderIdentityException.Hash("Failed to hash principal ID $principalId: ${ex.message}", ex) }
            .bind()
        val user = runSuspendCatching { repository.findByIdentity(provider, hashedPrincipalId) }
            .mapError { ex -> FindUserByProviderIdentityException.Database("Failed to find user by provider identity: ${ex.message}", ex) }
            .andThen { it.toResultOr { FindUserByProviderIdentityException.NotFound("No user found with provider identity $provider:$principalId") } }
            .bind()

        decrypt(user)
            .mapError { ex -> FindUserByProviderIdentityException.Encryption("Failed to decrypt user document: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Finds all users that have the specified role.
     *
     * @param role The [Role] to search for within the users.
     * @return A flow emitting [Result]s containing [User]s with the given role or an [EncryptionException] in case of decryption failure.
     */
    suspend fun findAllByRolesContaining(role: Role): Flow<Result<User, EncryptionException>> {
        logger.debug { "Finding all users with role $role" }

        return repository.findAllByRolesContaining(role).map { decrypt(it) }
    }

    /**
     * Finds all users whose group memberships contain the specified group identifier.
     *
     * @param group The group identifier to search for in user group memberships.
     * @return A [Flow] of [Result]s containing either the decrypted [User] or an [EncryptionException].
     */
    suspend fun findAllByGroupContaining(group: String): Flow<Result<User, EncryptionException>> {
        logger.debug { "Finding all users with group membership $group" }

        return repository.findAllByGroupsContaining(group).map { decrypt(it) }
    }

    /**
     * Fetches a paginated list of users based on various filter criteria.
     *
     * @param pageable the pagination information including page number, size, and sort order
     * @param email an email address to filter users, or null to ignore this filter
     * @param roles a set of roles to filter users, or null to ignore this filter
     * @param groups a set of groups to filter users, or null to ignore this filter
     * @param createdAtBefore a timestamp to filter users created before this date, or null to ignore this filter
     * @param createdAtAfter a timestamp to filter users created after this date, or null to ignore this filter
     * @param lastActiveBefore a timestamp to filter users who were last active before this date, or null to ignore this filter
     * @param lastActiveAfter a timestamp to filter users who were last active after this date, or null to ignore this filter
     * @param identityKeys a set of identity keys to filter users by associated identities, or null to ignore this filter
     * @return A [Result] result containing a [Page] of matching [User]s or an [FindAllEncryptedDocumentsPaginatedException] if an exception occurs
     */
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
    ): Result<Page<User>, FindAllEncryptedDocumentsPaginatedException> {
        logger.debug { "Finding ${encryptedDocumentClazz.simpleName}: page ${pageable.pageNumber}, size: ${pageable.pageSize}, sort: ${pageable.sort}" }

        val criteria = CriteriaBuilder()
            .isEqualTo(EncryptedUser::email, email?.let { hashService.hashSearchableHmacSha256(it) })
            .isIn(EncryptedUser::roles, roles)
            .isIn(EncryptedUser::groups, groups)
            .compare(EncryptedUser::createdAt, createdAtBefore, createdAtAfter)
            .compare(EncryptedUser::lastActive, lastActiveBefore, lastActiveAfter)
            .existsAny(identityKeys, EncryptedUser::identities)
            .build()

        return findAllPaginated(pageable, criteria)
    }

}
