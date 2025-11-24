package io.stereov.singularity.global.annotation

import io.stereov.singularity.global.exception.SingularityException
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ThrowsDomainError(
    val errorClasses: Array<KClass<out SingularityException>>
)
