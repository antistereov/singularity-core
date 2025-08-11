package io.stereov.singularity.user.device.config

import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.user.core.config.UserConfiguration
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.device.controller.UserDeviceController
import io.stereov.singularity.user.device.service.UserDeviceService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        UserConfiguration::class
    ]
)
class UserDeviceConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userDeviceController(
        userDeviceService: UserDeviceService
    ): UserDeviceController {
        return UserDeviceController(userDeviceService)
    }

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun userDeviceService(
        userService: UserService,
        authenticationService: AuthenticationService
    ) = UserDeviceService(userService, authenticationService)
}
