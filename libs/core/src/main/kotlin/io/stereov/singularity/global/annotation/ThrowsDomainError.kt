package io.stereov.singularity.global.annotation

import io.stereov.singularity.global.exception.SingularityException
import kotlin.reflect.KClass

/**
 * Annotation used to declare potential domain-specific exceptions that a function might throw.
 *
 * Applying this annotation to a function provides clear documentation and metadata about the
 * specific types of exceptions that can arise during the function's execution. The exceptions
 * must be subclasses of [SingularityException], which serves as the base class for domain-specific
 * exceptions used across the application.
 *
 * This annotation is particularly useful for generating runtime behavior, validation,
 * or documentation of possible errors within an API or service layer.
 *
 * @property errorClasses The array of exception classes that may be thrown by the annotated function.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ThrowsDomainError(
    val errorClasses: Array<KClass<out SingularityException>>
)
