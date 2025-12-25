package io.stereov.singularity.principal.group.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.principal.core.dto.response.PrincipalResponse
import io.stereov.singularity.principal.group.model.Group
import io.stereov.singularity.principal.group.model.GroupTranslation
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class GroupMemberControllerTest : BaseIntegrationTest(){

    @Test fun `addMember works`() = runTest {
        val admin = createAdmin()
        val user = registerUser()

        val group = groupService.save(
            Group(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
            )
        ).getOrThrow()

        val res = webTestClient.post()
            .uri("/api/groups/${group.key}/members/${user.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(PrincipalResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(setOf(group.key), res.groups)

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertEquals(mutableSetOf(group.key), updatedUser.groups)
    }
    @Test fun `addMember with non-existing group is not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser()

        webTestClient.post()
            .uri("/api/groups/new-pilots/members/${user.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertEquals(mutableSetOf<String>(), updatedUser.groups)
    }
    @Test fun `addMember requires authentication`() = runTest {
        val user = registerUser()

        val group = groupRepository.save(
            Group(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
            )
        )

        webTestClient.post()
            .uri("/api/groups/${group.key}/members/${user.id}")
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertEquals(mutableSetOf<String>(), updatedUser.groups)
    }
    @Test fun `addMember requires admin`() = runTest {
        val user = registerUser()

        val group = groupRepository.save(
            Group(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
            )
        )

        webTestClient.post()
            .uri("/api/groups/${group.key}/members/${user.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertEquals(mutableSetOf<String>(), updatedUser.groups)
    }
    @Test fun `addMember with non-existing user is not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser()
        userService.deleteById(user.id).getOrThrow()

        val group = groupRepository.save(
            Group(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
            )
        )

        webTestClient.post()
            .uri("/api/groups/${group.key}/members/${user.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test fun `removeMember works`() = runTest {
        val admin = createAdmin()
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(
            Group(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
            )
        )

        val res = webTestClient.delete()
            .uri("/api/groups/${group.key}/members/${user.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(PrincipalResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(emptySet<String>(), res.groups)

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertEquals(emptySet<String>(), updatedUser.groups)
    }
    @Test fun `removeMember with non-existing group is not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser(groups = listOf("pilots"))

        webTestClient.delete()
            .uri("/api/groups/new-pilots/members/${user.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertEquals(mutableSetOf("pilots"), updatedUser.groups)
    }
    @Test fun `removeMember requires authentication`() = runTest {
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(
            Group(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
            )
        )

        webTestClient.delete()
            .uri("/api/groups/${group.key}/members/${user.id}")
            .exchange()
            .expectStatus().isUnauthorized

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertEquals(mutableSetOf(group.key), updatedUser.groups)
    }
    @Test fun `removeMember requires admin`() = runTest {
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(
            Group(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
            )
        )

        webTestClient.delete()
            .uri("/api/groups/${group.key}/members/${user.id}")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        val updatedUser = userService.findById(user.id).getOrThrow()
        Assertions.assertEquals(mutableSetOf(group.key), updatedUser.groups)
    }
    @Test fun `removeMember with non-existing user is not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser(groups = listOf("pilots"))
        userService.deleteById(user.id).getOrThrow()

        val group = groupRepository.save(
            Group(
                null,
                "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
            )
        )

        webTestClient.delete()
            .uri("/api/groups/${group.key}/members/${user.id}")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }

}
