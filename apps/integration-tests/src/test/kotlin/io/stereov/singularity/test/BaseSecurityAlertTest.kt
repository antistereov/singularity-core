package io.stereov.singularity.test

import io.mockk.clearMocks
import io.stereov.singularity.auth.alert.service.IdentityProviderInfoService
import io.stereov.singularity.auth.alert.service.LoginAlertService
import io.stereov.singularity.auth.alert.service.NoAccountInfoService
import io.stereov.singularity.auth.alert.service.RegistrationAlertService
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.service.*
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
    @Autowired
    lateinit var registrationAlertService: RegistrationAlertService
    @Autowired
    lateinit var identityProviderInfoService: IdentityProviderInfoService
    @Autowired
    lateinit var noAccountInfoService: NoAccountInfoService


    @BeforeEach
    fun setupAlertMocks() {
        clearMocks(loginAlertService)
        clearMocks(securityAlertService)
        clearMocks(registrationAlertService)
        clearMocks(identityProviderInfoService)
        clearMocks(noAccountInfoService)
    }
}
