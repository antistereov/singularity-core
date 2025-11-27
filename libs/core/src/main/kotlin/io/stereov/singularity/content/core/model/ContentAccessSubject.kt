package io.stereov.singularity.content.core.model

/**
 * Represents the distinct types of subjects that can access or interact with a piece of content.
 *
 * This enum is primarily used to define the context within which permissions and roles
 * are granted or validated. Content access can be assigned to either individual users or groups.
 *
 * Types:
 * - `USER`: Corresponds to an individual user as the access subject.
 * - `GROUP`: Corresponds to a group of users as the access subject.
 *
 * Used in conjunction with access-related operations, such as sharing content or
 * assigning permissions, within the content management system.
 */
enum class ContentAccessSubject {
    USER, GROUP
}
