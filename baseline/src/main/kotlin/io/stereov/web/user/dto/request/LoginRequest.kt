package io.stereov.web.user.dto.request

/**
 * # LoginRequest data class.
 *
 * This data class represents a request for user login.
 * It includes the user's email, password, and device information.
 *
 * @property email The email address of the user.
 * @property password The password of the user.
 * @property device The device information associated with the login request.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class LoginRequest(
    val email: String,
    val password: String,
    val device: DeviceInfoRequest,
)
