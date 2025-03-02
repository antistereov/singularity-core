package io.stereov.web.user.repository

import io.stereov.web.user.model.UserDocument
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : CoroutineCrudRepository<UserDocument, String> {

    suspend fun existsByEmail(email: String): Boolean

    suspend fun findByEmail(email: String): UserDocument?
}
