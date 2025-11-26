package io.stereov.singularity.principal.group.model

import io.stereov.singularity.principal.group.model.KnownGroups.CONTRIBUTOR


/**
 * Represents a collection of known group keys, serving as constants for predefined group names.
 *
 * This object provides an easy reference for well-known group identifiers that might
 * correspond to entries in the `Group` collection within the database. These identifiers
 * can be used throughout the codebase for purposes such as access control, categorization,
 * and identification of specific group functionalities.
 *
 * @property CONTRIBUTOR A predefined key representing the "contributor" group.
 */
object KnownGroups {
    const val CONTRIBUTOR = "contributor"
}
