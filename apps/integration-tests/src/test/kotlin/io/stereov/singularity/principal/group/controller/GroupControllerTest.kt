package io.stereov.singularity.principal.group.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.principal.group.dto.request.CreateGroupRequest
import io.stereov.singularity.principal.group.dto.request.UpdateGroupRequest
import io.stereov.singularity.principal.group.dto.response.GroupResponse
import io.stereov.singularity.principal.group.model.Group
import io.stereov.singularity.principal.group.model.GroupTranslation
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.util.*

class GroupControllerTest() : BaseIntegrationTest() {

    data class GroupPage(
        val content: List<GroupResponse> = emptyList(),
        val pageNumber: Int,
        val pageSize: Int,
        val numberOfElements: Int,
        val totalElements: Long,
        val totalPages: Int,
        val first: Boolean,
        val last: Boolean,
        val hasNext: Boolean,
        val hasPrevious: Boolean
    )

    @Test fun `createGroup works`() = runTest {
        val admin = createAdmin()

        val req = CreateGroupRequest(
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        )

        val res = webTestClient.post()
            .uri("/api/groups")
            .accessTokenCookie(admin.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(req.key, res.key)
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.name, res.name)
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.description, res.description)

        groupRepository.deleteByKey("pilots")

        val resDE = webTestClient.post()
            .uri("/api/groups?locale=de")
            .accessTokenCookie(admin.accessToken)
            .bodyValue(req)
            .exchange()
            .expectBody(GroupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(resDE)

        Assertions.assertEquals(req.key, resDE.key)
        Assertions.assertEquals(req.translations[Locale.GERMAN]!!.name, resDE.name)
        Assertions.assertEquals(req.translations[Locale.GERMAN]!!.description, resDE.description)

        val group = groupRepository.findByKey(req.key)!!

        Assertions.assertEquals(req.key, group.key)
        Assertions.assertEquals(req.translations, group.translations)
    }
    @Test fun `createGroup requires authentication`() = runTest {
        val req = CreateGroupRequest(
            "pilots",
            mutableMapOf(),
        )

        webTestClient.post()
            .uri("/api/groups")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `createGroup requires admin`() = runTest {
        val user = registerUser()

        val req = CreateGroupRequest(
            "pilots",
            mutableMapOf(),
        )

        webTestClient.post()
            .uri("/api/groups")
            .accessTokenCookie(user.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isForbidden
    }
    @Test fun `createGroup needs unique key`() = runTest {
        val admin = createAdmin()

        val req = CreateGroupRequest(
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        )

        webTestClient.post()
            .uri("/api/groups")
            .accessTokenCookie(admin.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isOk

        webTestClient.post()
            .uri("/api/groups")
            .accessTokenCookie(admin.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }
    @Test fun `createGroup needs body`() = runTest {
        val admin = createAdmin()

        webTestClient.post()
            .uri("/api/groups")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `createGroup needs at least one translation`() = runTest {
        val admin = createAdmin()

        val req = CreateGroupRequest(
            "pilots",
            mutableMapOf(),
        )

        webTestClient.post()
            .uri("/api/groups")
            .accessTokenCookie(admin.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest
    }
    @Test fun `createGroup default locale needs translation`() = runTest {
        val admin = createAdmin()

        val req = CreateGroupRequest(
            "pilots",
            mutableMapOf(
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        )

        webTestClient.post()
            .uri("/api/groups")
            .accessTokenCookie(admin.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest

        val req2 = CreateGroupRequest(
            "pilots",
            mutableMapOf(
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        )

        webTestClient.post()
            .uri("/api/groups")
            .accessTokenCookie(admin.accessToken)
            .bodyValue(req2)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `getGroups works`() = runTest {
        val admin = createAdmin()

        groupRepository.save(Group(
            null,
            "pilots",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                    Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
                ),
        ))
        groupRepository.save(Group(
            null,
            "passengers",
                mutableMapOf(
                    Locale.ENGLISH to GroupTranslation("Passengers", "People who don't fly"),
                    Locale.GERMAN to GroupTranslation("Passagiere", "Menschen, die nicht fliegen")
                ),
        ))

        val res = webTestClient.get()
            .uri("/api/groups")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(2, res.content.size)
        Assertions.assertEquals(2, res.totalElements)
        Assertions.assertEquals(1, res.totalPages)
        Assertions.assertEquals(0, res.pageNumber)

        val pilots = res.content
            .find { it.key == "pilots" }
        requireNotNull(pilots)

        Assertions.assertEquals("Pilots", pilots.name)
        Assertions.assertEquals("People who fly", pilots.description)

        val passengers = res.content
            .find { it.key == "passengers" }
        requireNotNull(passengers)

        Assertions.assertEquals("Passengers", passengers.name)
        Assertions.assertEquals("People who don't fly", passengers.description)

        val resDE = webTestClient.get()
            .uri("/api/groups?locale=de")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(resDE)

        Assertions.assertEquals(2, resDE.content.size)
        Assertions.assertEquals(2, resDE.totalElements)
        Assertions.assertEquals(1, resDE.totalPages)
        Assertions.assertEquals(0, resDE.pageNumber)

        val pilotsDE = resDE.content
            .find { it.key == "pilots" }
        requireNotNull(pilotsDE)

        Assertions.assertEquals("Piloten", pilotsDE.name)
        Assertions.assertEquals("Menschen, die fliegen", pilotsDE.description)

        val passengersDE = resDE.content
            .find { it.key == "passengers" }
        requireNotNull(passengersDE)

        Assertions.assertEquals("Passagiere", passengersDE.name)
        Assertions.assertEquals("Menschen, die nicht fliegen", passengersDE.description)

        val resEN = webTestClient.get()
            .uri("/api/groups?locale=en")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectBody(GroupPage::class.java)
            .returnResult()
            .responseBody

        requireNotNull(resEN)

        Assertions.assertEquals(2, resEN.content.size)
        Assertions.assertEquals(2, resEN.totalElements)
        Assertions.assertEquals(1, resEN.totalPages)
        Assertions.assertEquals(0, resEN.pageNumber)

        val pilotsEN = resEN.content
            .find { it.key == "pilots" }
        requireNotNull(pilotsEN)

        Assertions.assertEquals("Pilots", pilotsEN.name)
        Assertions.assertEquals("People who fly", pilotsEN.description)

        val passengersEN = resEN.content
            .find { it.key == "passengers" }
        requireNotNull(passengersEN)

        Assertions.assertEquals("Passengers", passengersEN.name)
        Assertions.assertEquals("People who don't fly", passengersEN.description)
    }
    @Test fun `getGroups requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/groups")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `getGroups requires admin`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/groups")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden
    }

    @Test fun `getGroupByKey works`() = runTest {
        val admin = createAdmin()

        groupRepository.save(Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        ))

        val pilots = webTestClient.get()
            .uri("/api/groups/pilots")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(pilots)

        Assertions.assertEquals("Pilots", pilots.name)
        Assertions.assertEquals("People who fly", pilots.description)

        val pilotsDE = webTestClient.get()
            .uri("/api/groups/pilots?locale=de")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectBody(GroupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(pilotsDE)

        Assertions.assertEquals("Piloten", pilotsDE.name)
        Assertions.assertEquals("Menschen, die fliegen", pilotsDE.description)

        val pilotsEN = webTestClient.get()
            .uri("/api/groups/pilots?locale=en")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectBody(GroupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(pilotsEN)

        Assertions.assertEquals("Pilots", pilotsEN.name)
        Assertions.assertEquals("People who fly", pilotsEN.description)
    }
    @Test fun `getGroupByKey requires authentication`() = runTest {
        webTestClient.get()
            .uri("/api/groups/key")
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `getGroupByKey requires admin`() = runTest {
        val user = registerUser()

        webTestClient.get()
            .uri("/api/groups/key")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden
    }
    @Test fun `getGroupByKey returns not found when not existing`() = runTest {
        val admin = createAdmin()

        webTestClient.get()
            .uri("/api/groups/key")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound
    }

    @Test fun `updateGroup works`() = runTest {
        val admin = createAdmin()

        val group = Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        )

        groupRepository.save(group)

        val req = UpdateGroupRequest(
            translations = mutableMapOf(
                Locale.ENGLISH to GroupTranslation("PilotsNew", "People who flyNew")
            ),
            translationsToDelete = setOf(Locale.GERMAN),
        )

        val res = webTestClient.put()
            .uri("/api/groups/pilots")
            .bodyValue(req)
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk
            .expectBody(GroupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.name, res.name)
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.description, res.description)

        val updatedGroup = groupService.findByKey("pilots").getOrThrow()
        Assertions.assertTrue(updatedGroup.translations.size == 1)
        Assertions.assertTrue(updatedGroup.translations.contains(Locale.ENGLISH))
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.name, updatedGroup.translations[Locale.ENGLISH]!!.name)
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.description, updatedGroup.translations[Locale.ENGLISH]!!.description)

        val resDE = webTestClient.put()
            .uri("/api/groups/pilots?locale=de")
            .bodyValue(req)
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectBody(GroupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(resDE)

        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.name, resDE.name)
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.description, resDE.description)
    }
    @Test fun `updateGroup delete and put is okay`() = runTest {
        val admin = createAdmin()

        val group = Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
            ),
        )

        groupRepository.save(group)

        val req = UpdateGroupRequest(
            translations = mutableMapOf(
                Locale.ENGLISH to GroupTranslation("PilotsNew", "People who flyNew")
            ),
            translationsToDelete = setOf(Locale.ENGLISH, Locale.GERMAN),
        )

        val res = webTestClient.put()
            .uri("/api/groups/pilots")
            .bodyValue(req)
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectBody(GroupResponse::class.java)
            .returnResult()
            .responseBody

        requireNotNull(res)

        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.name, res.name)
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.description, res.description)

        val updatedGroup = groupService.findByKey("pilots").getOrThrow()
        Assertions.assertTrue(updatedGroup.translations.size == 1)
        Assertions.assertTrue(updatedGroup.translations.contains(Locale.ENGLISH))
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.name, updatedGroup.translations[Locale.ENGLISH]!!.name)
        Assertions.assertEquals(req.translations[Locale.ENGLISH]!!.description, updatedGroup.translations[Locale.ENGLISH]!!.description)
    }
    @Test fun `updateGroup default locale needs translation`() = runTest {
        val admin = createAdmin()

        val group = groupRepository.save(Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        ))

        val req = UpdateGroupRequest(
            translationsToDelete = setOf(Locale.ENGLISH),
            translations = mutableMapOf()
        )

        webTestClient.put()
            .uri("/api/groups/pilots")
            .accessTokenCookie(admin.accessToken)
            .bodyValue(req)
            .exchange()
            .expectStatus().isBadRequest

        val updatedGroup = groupService.findByKey(group.key).getOrThrow()

        Assertions.assertEquals(group.id, updatedGroup.id)
        Assertions.assertEquals(group.translations, updatedGroup.translations)
    }
    @Test fun `updateGroup requires authentication`() = runTest {
        val group = Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        )

        groupRepository.save(group)

        val req = UpdateGroupRequest(
            translations = mutableMapOf(
                Locale.ENGLISH to GroupTranslation("PilotsNew", "People who flyNew")
            ),
            translationsToDelete = setOf(Locale.GERMAN),
        )

        webTestClient.put()
            .uri("/api/groups/pilots")
            .bodyValue(req)
            .exchange()
            .expectStatus().isUnauthorized
    }
    @Test fun `updateGroup requires admin`() = runTest {
        val user = registerUser()

        val group = Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        )

        groupRepository.save(group)

        val req = UpdateGroupRequest(
            translations = mutableMapOf(
                Locale.ENGLISH to GroupTranslation("PilotsNew", "People who flyNew")
            ),
            translationsToDelete = setOf(Locale.GERMAN),
        )

        webTestClient.put()
            .uri("/api/groups/pilots")
            .bodyValue(req)
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden
    }
    @Test fun `updateGroup requires body`() = runTest {
        val admin = createAdmin()

        val group = Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        )

        groupRepository.save(group)

        webTestClient.put()
            .uri("/api/groups/pilots")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test fun `deleteGroup works`() = runTest {
        val admin = createAdmin()
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        ))

        webTestClient.delete()
            .uri("/api/groups/pilots")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isOk

        Assertions.assertFalse(groupService.existsByKey(group.key).getOrThrow())
        Assertions.assertFalse(groupService.existsById(group.id.getOrThrow()).getOrThrow())

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertTrue(updatedUser.groups.isEmpty())
    }
    @Test fun `deleteGroup requires authentication`() = runTest {
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        ))

        webTestClient.delete()
            .uri("/api/groups/pilots")
            .exchange()
            .expectStatus().isUnauthorized

        Assertions.assertTrue(groupService.existsByKey(group.key).getOrThrow())
        Assertions.assertTrue(groupService.existsById(group.id.getOrThrow()).getOrThrow())

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertEquals(setOf(group.key),updatedUser.groups)
    }
    @Test fun `deleteGroup requires admin`() = runTest {
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        ))

        webTestClient.delete()
            .uri("/api/groups/pilots")
            .accessTokenCookie(user.accessToken)
            .exchange()
            .expectStatus().isForbidden

        Assertions.assertTrue(groupService.existsByKey(group.key).getOrThrow())
        Assertions.assertTrue(groupService.existsById(group.id.getOrThrow()).getOrThrow())

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertEquals(setOf(group.key),updatedUser.groups)
    }
    @Test fun `deleteGroup returns not found`() = runTest {
        val admin = createAdmin()
        val user = registerUser(groups = listOf("pilots"))

        val group = groupRepository.save(Group(
            null,
            "pilots",
            mutableMapOf(
                Locale.ENGLISH to GroupTranslation("Pilots", "People who fly"),
                Locale.GERMAN to GroupTranslation("Piloten", "Menschen, die fliegen")
            ),
        ))

        webTestClient.delete()
            .uri("/api/groups/no-pilots")
            .accessTokenCookie(admin.accessToken)
            .exchange()
            .expectStatus().isNotFound

        Assertions.assertTrue(groupService.existsByKey(group.key).getOrThrow())
        Assertions.assertTrue(groupService.existsById(group.id.getOrThrow()).getOrThrow())

        val updatedUser = userService.findById(user.id).getOrThrow()

        Assertions.assertEquals(setOf(group.key),updatedUser.groups)
    }

}
