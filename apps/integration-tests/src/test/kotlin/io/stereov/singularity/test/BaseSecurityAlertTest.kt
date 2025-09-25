package io.stereov.singularity.test

import io.mockk.clearMocks
import io.stereov.singularity.auth.core.service.LoginAlertService
import io.stereov.singularity.auth.core.service.SecurityAlertService
import io.stereov.singularity.test.config.MockSecurityAlertConfig
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import

@Import(MockSecurityAlertConfig::class)
class BaseSecurityAlertTest : BaseMailIntegrationTest() {

    @Autowired
    lateinit var loginAlertService: LoginAlertService
    @Autowired
    lateinit var securityAlertService: SecurityAlertService

    @BeforeEach
    fun setupAlertMocks() {
        clearMocks(loginAlertService)
        clearMocks(securityAlertService)
    }
}