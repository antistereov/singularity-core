package io.stereov.web.config

/**
 * # Constants for web application
 *
 * This object contains constants used throughout the web application, including cookie names and JWT claims.
 *
 * @property ACCESS_TOKEN_COOKIE The name of the access token cookie.
 * @property REFRESH_TOKEN_COOKIE The name of the refresh token cookie.
 * @property TWO_FACTOR_AUTH_COOKIE The name of the two-factor authentication cookie.
 * @property STEP_UP_TOKEN_COOKIE The name of the step-up token cookie.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
object Constants {
    const val ACCESS_TOKEN_COOKIE = "access_token"
    const val REFRESH_TOKEN_COOKIE = "refresh_token"
    const val TWO_FACTOR_AUTH_COOKIE = "2fa_token"
    const val JWT_DEVICE_CLAIM = "device"
    const val STEP_UP_TOKEN_COOKIE = "step_up_token"
}
