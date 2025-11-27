package io.stereov.singularity.content.core.config

import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.controller.ContentManagementController
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.core.service.ContentManagementService
import io.stereov.singularity.email.core.config.EmailConfiguration
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
        AuthenticationConfiguration::class,
        EmailConfiguration::class
    ]
)
@EnableConfigurationProperties(ContentProperties::class)
class ContentConfiguration {

    @Autowired
    private lateinit var context: ApplicationContext

    @PostConstruct
    fun checkManagementServices() {
        val servicesWithKeys = context.getBeansOfType<ContentManagementService<*>>().values
            .map { service -> service to service.contentType }
        val servicesGroupedByKey = servicesWithKeys.groupBy { it.second }
        val duplicateKeyEntries = servicesGroupedByKey
            .filter { (_, list) -> list.size > 1 }

        if (duplicateKeyEntries.isNotEmpty()) {
            val duplicatesInfo = duplicateKeyEntries.entries.joinToString(separator = "\n") { (key, entries) ->
                val beanNames = entries.joinToString(", ") { it.first::class.qualifiedName.toString() }
                " -> Key '$key' is used by the following services: $beanNames"
            }

            throw IllegalStateException(
                "Critical Configuration Error: Multiple ContentManagementServices use the same Content Key!\n" +
                        "Please ensure every service has a unique 'contentKey'.\n" +
                        "Details:\n$duplicatesInfo"
            )
        }
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun contentManagementController(
        context: ApplicationContext
    ) = ContentManagementController(
        context
    )

    // Component
    
    @Bean
    @ConditionalOnMissingBean
    fun accessCriteria(authorizationService: AuthorizationService): AccessCriteria {
        return AccessCriteria(authorizationService)
    }
}
