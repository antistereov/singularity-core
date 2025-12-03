---
sidebar_position: 2
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
* Using the [`AuthorizationService`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/auth/core/service/AuthorizationService.kt) for fine-grained authorization _(more infos [here](#authorization-through-authorizationservice))_.
* Defining required roles through the configuration _(more infos [here](#authorization-through-configuration))_.

:::warning
The preferred way is by specifying the requirements using the [`AuthorizationService`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/auth/core/service/AuthorizationService.kt).
This way the requirements are readable in code and simplify code maintenance.
It also allows setting more specific requirements such as group memberships.
:::

## Authorization through AuthorizationService

The [`AuthorizationService`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/auth/core/service/AuthorizationService.kt) provides an easy way to sure endpoints at the controller level.

:::info
*Singularity* uses typed exceptions using a custom `Result` type.
You can read more about this [here](../design-decisions.md).

In short, every service that can throw an exception returns a `Result<V,E>` where `V` is the class of the
expected value and `E` is a subtype of `SingularityException`. Each of these exceptions are already automatically
mapped to an HTTP-error response.
:::

### Requiring a Role

Let's say you have two endpoints: 
* `/api/cool-stuff` that should return cool stuff everybody should see.
* `/api/cool-stuff/for-admins` that only admins can see.

You fetch the cool stuff using `CoolStuff.forEveryone()` and `CoolStuff.forAdmins()`.
The endpoint that publishes `CoolStuff` that should only be visible by `ADMIN`s should therefore be secured.

You can implement it this way:

```kotlin
/**
 * This is a simple controller that returns cool stuff.
 */
@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val service: CoolStuffService,
    private val authorizationService: AuthorizationService
) {

    /**
     * This endpoint is public and provides everybody with cool stuff.
     */
    @GetMapping
    suspend fun getCoolStuff(): ResponseEntity<CoolStuff> {
        return ResponseEntity.ok(service.getCoolStuff())
    }

    /**
     * This endpoint is only accessible for users with the role `ADMIN`.
     * We use the `AuthorizationService` to require a role here.
     */
    @GetMapping("for-admins")
    suspend fun getCoolStuffForAdmins(): ResponseEntity<CoolStuff> {
        // We first get the authentication outcome that checks the access token
        authorizationService.getAuthenticationOutcome()
            // If it fails, we throw this exception
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } } // This explicit getOrThrow is done so no error is thrown "magically"
            // To be an admin, at first, you need to be authenticated
            .requireAuthentication()
            // If the user is not authenticated, we throw an exception
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            // Now since we have an authenticated user, we can check if the ADMIN role is present
            .requireRole(Role.User.ADMIN)
            // If not, we throw another exception
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }
        
        
        return ResponseEntity.ok(service.getCoolStuffForAdmins)
    }
}

@Service
class CoolStuffService() {
    
    suspend fun getCoolStuff(): CoolStuff {
        return CoolStuff.forEveryone()
    }
    
    /**
     * No require role here. This allows straight-forward testing without setting up an authentication session.
     */
    suspend fun getCoolStuffForAdmins(): CoolStuff {
        return CoolStuff.forAdmins()
    }
}

```

If you call `/api/cool-stuff/for-admins` now, you get:
* `200` if you are provided a valid token for a user with `ADMIN` role.
* `401` if you didn't provide a valid [`AccessToken`](tokens.md#access-token).
* `403` if you provided a valid token, but the user doesn't have the `ADMIN` role.

### Requiring Group Membership

You can be more fine-grained in your requirements.
Define and use custom groups and limit access to members of this group only.

:::note
Everything about creating and using groups is covered here: [Groups](./groups).
:::

```kotlin
/**
 * An interface with your group keys to reuse the names across the code.
 */
interface GroupKeys {
    const val COOL_GROUP = "cool-group"
}

@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val service: CoolStuffService,
    private val authorizationService: AuthorizationService
) {

    /**
     * This endpoint is only accessible for users who are members of the group `cool-group`.
     */
    @GetMapping("for-cool-group")
    suspend fun getCoolStuffForCoolGroup(): ResponseEntity<CoolStuff> {
        // We first get the authentication outcome that checks the access token
        authorizationService.getAuthenticationOutcome()
            // If it fails, we throw this exception
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            // To be a member of a group, at first, you need to be authenticated
            .requireAuthentication()
            // If the user is not authenticated, we throw an exception
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            // Now since we have an authenticated user, we can check if the user is a member of our cool group
            .requireGroupMembership(GroupKeys.COOL_GROUP)
            // And throw an exception if that's not the case
            .getOrThrow { when (it) { is AuthenticationException.GroupMembershipRequired -> it } }
        
        return ResponseEntity.ok(service.getCoolStuffForCoolGroup)
    }
}

@Service
class CoolStuffService() {

    /**
     * Again no check here, the check is moved to the controller.
     */
    suspend fun getCoolStuffForCoolGroup(): CoolStuff {
        return CoolStuff.forAdmins()
    }
}
```

If you call `/api/cool-stuff/for-cool-group` now, you get:
* `200` if you are provided a valid token for a user who is a member of `cool-group`.
* `401` if you didn't provide a valid [`AccessToken`](tokens.md#access-token).
* `403` if you provided a valid token, but the user is not a member of `cool-group`.

### Getting User Information

Let's say you have specific cool stuff prepared for each user using `CoolStuff.forUserWithId(..)`.

```kotlin
@Service
class CoolStuffService() {

    suspend fun getCoolStuffForUserWithId(id: ObjectId): CoolStuff {
        return CoolStuff.forUserWithId(id)
    }
}

@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val service: CoolStuffService,
    private val authorizationService: AuthorizationService
) {

    @GetMapping
    suspend fun getCoolStuffForCurrentUser(): ResponseEntity<CoolStuff> {
        // These steps are similar, now this time we save the outcome to a variable
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
        
        // We retrieve the current user's ID from the authentication outcome
        val currentUserId = authenticationOutcome.principalId
        
        // We can call the service now to retrieve the personalized cool stuff
        return ResponseEntity.ok(service.getCoolStuffForUserWithId(id))
    }
}
```

If you call `/api/cool-stuff` now, you get:
* `200` if you are provided a valid token for a user.
* `401` if you didn't provide a valid token.

### Requiring Step-Up Authentication

You can secure critical endpoints by requiring the user to reauthenticate.

:::note
You can learn more about the step-up authentication flow [here](./authentication.md#step-up).
:::

```kotlin
@Service
class CoolStuffService() {

    suspend fun removeCoolStuffFromUser(user: User) {
        user.removeCoolStuff()
    }
}

@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val service: CoolStuffService,
    private val authorizationService: AuthorizationService,
    private val userService: UserService
) {

    /**
     * This endpoint removes cool stuff from the user.
     * This is a security-critical action.
     */
    @DeleteMapping
    suspend fun removeCoolStuffFromUser(
        // We need to inject the ServerWebExchange to extract the StepUp Token
        exchange: ServerWebExchange
    ): ResponseEntity<CoolStuff> {
        // This part we know already
        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) {is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) {is AuthenticationException.AuthenticationRequired -> it } }

        // No we use the ServerWebExchange and the AuthenticationOutcome
        authorizationService.requireStepUp(authenticationOutcome, exchange)
            // ...and throw the exception if the user did not perform a step-up
            .getOrThrow { when (it) {is StepUpTokenExtractionException -> it } }

        // We retrieve the user from the database and throw the related exception if it fails
        val user = userService.findById(authenticationOutcome.principalId)
            .getOrThrow { FindUserByIdException.from(it) }

        // Now we can call the method and we are sure that the user is authenticated and performed a step-up
        return ResponseEntity.ok(service.removeCoolStuffFromUser(user))
    }
}
```

If you call `/api/cool-stuff` now, you get:
* `200` whether you provide a token or not.

## Authorization Through Configuration

If you decide to secure endpoints through configuration, you can define them using the following properties.

### Properties

| Property                      | Type           | Description                                                                | Default value |
|-------------------------------|----------------|----------------------------------------------------------------------------|---------------|
| singularity.auth.public-paths | `List<String>` | Paths that do not require authentication.                                  |               |
| singularity.auth.user-paths   | `List<String>` | Paths that require users to be authenticated and to have the `USER` role.  |               |
| singularity.auth.admin-paths  | `List<String>` | Paths that require users to be authenticated and to have the `ADMIN` role. |               |

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
