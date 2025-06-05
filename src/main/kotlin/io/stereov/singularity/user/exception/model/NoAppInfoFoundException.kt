package io.stereov.singularity.user.exception.model

import io.stereov.singularity.user.exception.UserException
import org.bson.types.ObjectId

/**
 * # No application info found exception.
 *
 * This exception is thrown when no application info is found for a user.
 *
 * @param userId The ID of the user for whom no application info was found.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class NoAppInfoFoundException(userId: ObjectId) : UserException(
    message = "No application info found for user $userId"
)
