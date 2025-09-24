# Security

*Singularity* provides built-in security features to protect your application, 
focusing on authentication, authorization, and rate limiting. 
The security configuration is designed to be easily managed through `application.yml` properties.

## Rate Limiting

Rate limiting is a key security measure to prevent abuse and denial-of-service attacks. 
*Singularity* applies rate limits at multiple levels using a `RateLimitFilter`.

## Configuration

### Rate Limiting

You can configure rate limits in your `application.yml` file using the following properties:

| Property                                                | Type           | Description                                                                                                                                            | Default Value |
|:--------------------------------------------------------|:---------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------|
| singularity.security.rate-limit.ip-limit                | `Long`         | The maximum number of requests per minute from a single IP address.                                                                                    | `200`         |
| singularity.security.rate-limit.ip-time-window          | `Long`         | The time window for the IP-based rate limit in minutes.                                                                                                | `1`           |
| singularity.security.rate-limit.user-limit              | `Long`         | The maximum number of requests per minute for an authenticated user.                                                                                   | `200`         |
| singularity.security.rate-limit.user-time-window        | `Long`         | The time window for the user-based rate limit in minutes.                                                                                              | `1`           |
| singularity.security.login-attempt-limit.ip-limit       | `Long`         | The maximum number of login attempts allowed from a single IP address.                                                                                 | `10`          |
| singularity.security.login-attempt-limit.ip-time-window | `Long`         | The time window in minutes for the IP-based login attempt limit.                                                                                       | `5`           |


The following authentication-related endpoints are subject to these login attempt limits: 
* [`POST /api/auth/login`](../api/login.api.mdx)
* [`POST /api/auth/step-up`](../api/step-up.api.mdx)
* [`POST /api/auth/2fa/totp/recover`](../api/recover-from-totp.api.mdx)
* [`POST /api/auth/password/reset`](../api/reset-password.api.mdx)
* [`POST /api/auth/2fa/login`](../api/complete-login.api.mdx)
* [`POST /api/auth/2fa/step-up`](../api/complete-step-up.api.mdx)

### CORS (Cross-Origin Resource Sharing)

The framework includes a pre-configured `CorsConfigurationSource` to handle CORS requests. 
By default, it allows `GET`, `POST`, `PUT`, `DELETE`, and `OPTIONS` methods, 
as well as `Authorization` and `Content-Type` headers.

| Property                                                | Type           | Description                                                                                                                                            | Default Value |
|:--------------------------------------------------------|:---------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------|:--------------|
| singularity.security.allowed-origins                    | `List<String>` | A list of allowed origins for Cross-Origin Resource Sharing (CORS). Requests from origins not on this list will be rejected.                           | `emptyList()` |


### Example `application.yaml`

```yaml
singularity:
  security:
    allowed-origins:
      - "https://your-frontend.com"
      - "http://localhost:3000"
  rate-limit:
    ip-limit: 300
    user-limit: 500
  login-attempt-limit:
    ip-limit: 5
    ip-time-window: 10
```