---
sidebar_position: 3
description: Learn how to secure an endpoint.
---

# Securing Endpoints

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

:::info
All paths are public by default.
:::

There are two ways to secure your endpoints:

* Using the `AuthorizationService` for fine-grained authorization *(more info [here](#authorization-through-authorizationservice))*.
* Defining required roles through the configuration *(more info [here](#authorization-through-configuration))*.

:::warning
The preferred way is by specifying the requirements using the `AuthorizationService`. This way the requirements are readable in code and simplify code maintenance. It also allows setting more specific requirements such as group memberships and step-up authentication.
:::

## Authorization through `AuthorizationService`

The `AuthorizationService` provides an easy way to secure endpoints at the controller level.

### The `AuthenticationOutcome` Model

The **`AuthenticationOutcome`** sealed class represents the final result of the authentication process for any request. It is the core object you will use to make authorization decisions in your controllers.

| Outcome Class       | Description                                                                                                                                                                                        | Authentication Status |
|:--------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:----------------------|
| **`Authenticated`** | The request was successfully authenticated. The outcome contains the **`principalId`** (the `ObjectId` of the `User` or `Guest`), **`sessionId`**, **`roles`**, and **`groups`** of the principal. | ✅ Authenticated       |
| **`None`**          | The request has no valid authentication token or credentials. The principal is anonymous.                                                                                                          | ❌ Unauthenticated     |

### 1. How to Get the `AuthenticationOutcome`

You retrieve the outcome using the **`AuthorizationService`** in your controller methods:

```kotlin
@RestController
class MyController(
    private val authorizationService: AuthorizationService,
) {
    @GetMapping("/api/my-resource")
    suspend fun getMyResource(): ResponseEntity<String> {
        val outcome = authorizationService.getAuthenticationOutcome()
            // We use getOrThrow with an explicit type cast here for better maintainability
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } } // Handles token extraction errors
        
        // The 'outcome' is now either AuthenticationOutcome.Authenticated or AuthenticationOutcome.None
        
        // ... proceed with authorization logic
    }
}
```

The method signature in `AuthorizationService` is:

```kotlin
// In AuthorizationService.kt
suspend fun getAuthenticationOutcome(): Result<AuthenticationOutcome, AccessTokenExtractionException>
```

This method checks the Reactive Security Context for the authentication token and wraps it into the appropriate `AuthenticationOutcome` class.

### 2. Requiring Authentication

To ensure only logged-in users or guests can access an endpoint, you call **`requireAuthentication()`** on the `AuthenticationOutcome`.

| Method                        | Description                                                                                |
|:------------------------------|:-------------------------------------------------------------------------------------------|
| **`requireAuthentication()`** | Fails with `401 Unauthorized` if the outcome is `None`. Returns `Authenticated` otherwise. |

#### Example: Requiring Any Principal

```kotlin
@GetMapping("/api/secured/info")
suspend fun getPrincipalInfo(): ResponseEntity<String> {
    val outcome = authorizationService.getAuthenticationOutcome()
        .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

    // This ensures the outcome is 'Authenticated'. If not, an AuthenticationException.AuthenticationRequired is thrown
    // which translates to an HTTP 401 Unauthorized response.
    val authenticatedOutcome = outcome.requireAuthentication()
        .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

    val principalId = authenticatedOutcome.principalId
    return ResponseEntity.ok("Welcome Principal $principalId!")
}
```

### 3. Requiring Roles and Group Membership

Once you have an `Authenticated` outcome, you can enforce more granular access control using its specialized methods. If the check fails, an exception is thrown which translates to an HTTP `403 Forbidden` response.

| Method                                         | Description                                                                                                           | Error Thrown                                      | Status          | Error Code                  |
|:-----------------------------------------------|:----------------------------------------------------------------------------------------------------------------------|:--------------------------------------------------|-----------------|-----------------------------|
| **`requireRole(role: Role)`**                  | Checks if the principal has the specified `Role`. **`ADMIN`**s automatically satisfy any role requirement.            | `AuthenticationException.RoleRequired`            | `403 Forbidden` | `ROLE_REQUIRED`             |
| **`requireGroupMembership(groupKey: String)`** | Checks if the principal is a member of the specified group. **`ADMIN`**s automatically satisfy any group requirement. | `AuthenticationException.GroupMembershipRequired` | `403 Forbidden` | `GROUP_MEMBERSHIP_REQUIRED` |

#### Example: Requiring `ADMIN` Role

```kotlin
@DeleteMapping("/api/users/{userId}")
suspend fun deleteUser(@PathVariable userId: String): ResponseEntity<Unit> {
    val outcome = authorizationService.getAuthenticationOutcome()
        .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

    // 1. Ensure the user is authenticated (401)
    val authenticatedOutcome = outcome.requireAuthentication()
        .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

    // 2. Ensure the authenticated principal has the ADMIN role (403)
    authenticatedOutcome.requireRole(Role.User.ADMIN)
        .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

    // userService.deleteById(ObjectId(userId))
    return ResponseEntity.noContent().build()
}
```

#### Example: Requiring Group Membership

```kotlin
@PostMapping("/api/projects/{projectId}/settings")
suspend fun updateProjectSettings(@PathVariable projectId: String): ResponseEntity<Unit> {
    val outcome = authorizationService.getAuthenticationOutcome()
        .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

    val authenticatedOutcome = outcome.requireAuthentication()
        .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

    // The principal (or an ADMIN) must be in the 'project:$projectId' group to manage settings
    authenticatedOutcome.requireGroupMembership("project:$projectId")
        .getOrThrow { when (it) { is AuthenticationException.GroupMembershipRequired -> it } }
    
    // projectService.updateSettings(...)
    return ResponseEntity.ok().build()
}
```

### 4. Requiring Step-Up Authentication

:::note
You can learn more about step-up authentication [here](./authentication.md#step-up).
:::

**Step-Up Authentication** (or re-authentication) is a security feature that requires a user to re-verify their identity 
(e.g., by entering their password or a 2FA code) before performing a highly sensitive action.

The `AuthorizationService` provides the **`validateStepUp()`** method to enforce this check. It looks for a valid Step-Up Token (a short-lived token generated after a successful re-authentication) in the request headers or cookies.

| Method                                                                          | Description                                                                         | Error Thrown (403)               |
|:--------------------------------------------------------------------------------|:------------------------------------------------------------------------------------|:---------------------------------|
| **`requireStepUp(authentication: Authenticated, exchange: ServerWebExchange)`** | Checks for a valid Step-Up Token associated with the current session and principal. | `StepUpTokenExtractionException` |

#### Example: Requiring Step-Up

```kotlin
@PostMapping("/api/cool-stuff")
suspend fun removeCoolStuff(
    // The ServerWebExchange will be injected by Spring automatically
    exchange: ServerWebExchange
): ResponseEntity<String> {
    val outcome = authorizationService.getAuthenticationOutcome()
        .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }

    // 1. Ensure the user is authenticated (401)
    val authenticatedOutcome = outcome.requireAuthentication()
        .getOrThrow { AuthorizationException.from(it) }

    // 2. Ensure the user performed a step-up (403)
    // The ServerWebExchange is required for token extraction
    authorizationService.validateStepUp(authenticatedOutcome, exchange)
        .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

    // 3. Retrieve the Principal (User or Guest) using the ID from the outcome
    // (A common pattern to get the full principal object)
    val principal = principalService.findById(authenticatedOutcome.principalId)
        .getOrThrow { when (it) { is FindPrincipalByIdException -> it } }

    // Now we can call the method, and we are sure that the principal is authenticated and performed a step-up
    return ResponseEntity.ok(service.removeCoolStuff(principal))
}
```

## Authorization Through Configuration

If you decide to secure endpoints through configuration, you can define them using the following properties.

### Properties

| Property                        | Type           | Description                                                                    | Default value |
|:--------------------------------|:---------------|:-------------------------------------------------------------------------------|:--------------|
| `singularity.auth.public-paths` | `List<String>` | Paths that do not require authentication.                                      |               |
| `singularity.auth.user-paths`   | `List<String>` | Paths that require users to be authenticated and to have the **`USER`** role.  |               |
| `singularity.auth.admin-paths`  | `List<String>` | Paths that require users to be authenticated and to have the **`ADMIN`** role. |               |

#### Example

```yaml
singularity:
  auth:
    public-paths:
      - /api/public/path
    user-paths:
      - /api/user/path
      - /api/user/another-path
    admin-paths:
      - /api/admins/path
```