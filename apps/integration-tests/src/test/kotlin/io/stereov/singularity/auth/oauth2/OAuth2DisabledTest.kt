package io.stereov.singularity.auth.oauth2

import io.stereov.singularity.auth.oauth2.controller.OAuth2ProviderController
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.auth.oauth2.controller.IdentityProviderController
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBeansOfType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class OAuth2DisabledTest : BaseIntegrationTest() {

    @Test
    fun `controller should be disabled`() = runTest {
        Assertions.assertTrue(applicationContext.getBeansOfType<OAuth2ProviderController>().isEmpty())
        Assertions.assertTrue(applicationContext.getBeansOfType<IdentityProviderController>().isEmpty())
    }

    companion object {
        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.oauth2.enable") { false }
        }
    }
}
