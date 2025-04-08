package io.stereov.web.user.dto.response

/**
 * # StepUpStatusResponse data class.
 *
 * This data class represents the response for the step-up status check.
 * It contains a single property, `stepUp`, which indicates whether a step-up authentication is set.
 *
 * @property stepUp A boolean value indicating whether step-up authentication is set.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class StepUpStatusResponse(
    val stepUp: Boolean,
)
