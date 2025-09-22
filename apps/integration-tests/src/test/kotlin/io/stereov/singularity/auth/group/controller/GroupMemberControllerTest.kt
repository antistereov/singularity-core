package io.stereov.singularity.auth.group.controller

import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.group.model.GroupDocument
import io.stereov.singularity.auth.group.model.GroupTranslation
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.user.core.dto.response.UserResponse
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class GroupMemberControllerTest : BaseIntegrationTest(){

    @Test fun `addMember works`() = runTest {
        val admin = createAdmin()
        val user = registerUser()

        val group = groupRepository.save(
            GroupDocument(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
                appProperties.locale
            )
        )

        val res = webTestClient.post()
            .uri("/api/groups/${group.key}/members/${user.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(setOf(group.key), res.groups)

        val updatedUser = userService.findById(user.info.id)

        Assertions.assertEquals(mutableSetOf(group.key), updatedUser.groups)
    }
    @Test fun `addMember with non-existing group is not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser()

        webTestClient.post()
            .uri("/api/groups/new-pilots/members/${user.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound

        val updatedUser = userService.findById(user.info.id)

        Assertions.assertEquals(mutableSetOf<String>(), updatedUser.groups)
    }
    @Test fun `addMember requires authentication`() = runTest {
        val user = registerUser()

        val group = groupRepository.save(
            GroupDocument(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
                appProperties.locale
            )
        )

        webTestClient.post()
            .uri("/api/groups/${group.key}/members/${user.info.id}")
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)

        Assertions.assertEquals(mutableSetOf<String>(), updatedUser.groups)
    }
    @Test fun `addMember requires admin`() = runTest {
        val user = registerUser()

        val group = groupRepository.save(
            GroupDocument(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
                appProperties.locale
            )
        )

        webTestClient.post()
            .uri("/api/groups/${group.key}/members/${user.info.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val updatedUser = userService.findById(user.info.id)

        Assertions.assertEquals(mutableSetOf<String>(), updatedUser.groups)
    }
    @Test fun `addMember with non-existing user is not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser()
        userService.deleteById(user.info.id)

        val group = groupRepository.save(
            GroupDocument(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
                appProperties.locale
            )
        )

        webTestClient.post()
            .uri("/api/groups/${group.key}/members/${user.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test fun `removeMember works`() = runTest {
        val admin = createAdmin()
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(
            GroupDocument(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
                appProperties.locale
            )
        )

        val res = webTestClient.delete()
            .uri("/api/groups/${group.key}/members/${user.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(UserResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(emptySet<String>(), res.groups)

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(emptySet<String>(), updatedUser.groups)
    }
    @Test fun `removeMember with non-existing group is not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser(groups = listOf("pilots"))

        webTestClient.delete()
            .uri("/api/groups/new-pilots/members/${user.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(mutableSetOf("pilots"), updatedUser.groups)
    }
    @Test fun `removeMember requires authentication`() = runTest {
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(
            GroupDocument(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
                appProperties.locale
            )
        )

        webTestClient.delete()
            .uri("/api/groups/${group.key}/members/${user.info.id}")
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(mutableSetOf(group.key), updatedUser.groups)
    }
    @Test fun `removeMember requires admin`() = runTest {
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(
            GroupDocument(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
                appProperties.locale
            )
        )

        webTestClient.delete()
            .uri("/api/groups/${group.key}/members/${user.info.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val updatedUser = userService.findById(user.info.id)
        Assertions.assertEquals(mutableSetOf(group.key), updatedUser.groups)
    }
    @Test fun `removeMember with non-existing user is not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser(groups = listOf("pilots"))
        userService.deleteById(user.info.id)

        val group = groupRepository.save(
            GroupDocument(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
                appProperties.locale
            )
        )

        webTestClient.delete()
            .uri("/api/groups/${group.key}/members/${user.info.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }

}