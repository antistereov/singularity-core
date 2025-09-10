---
sidebar_position: 1
description: Learn how to secure an endpoint.
---

# Basics

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

Authorization is one of the core features of *Singularity*.
Every important endpoint for user registration, login and session control is already built-in.
Useful features such as mail verification and two-factor authentication are included out of the box.

Learn how you can use the predefined endpoints to authorize your users.

## Securing Endpoints

:::info
All paths are public by default.
:::

There are two ways to secure your endpoints:
* Using the [`AuthorizationService`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/auth/core/service/AuthorizationService.kt) for authorization on the [service level](#authorization-at-service-level).
* Defining required roles [by path](#authorization-by-path) as an easy option to secure endpoints.

:::warning
The preferred way is by specifying the requirements on the service level.
Therefore, every endpoint that uses a service method that requires authorization will be secured by default.
It also allows setting more specific requirements such as group memberships.
:::

## Authorization at Service Level

The [`AuthorizationService`](https://github.com/antistereov/singularity-core/blob/main/src/main/kotlin/io/stereov/singularity/auth/core/service/AuthorizationService.kt) provides an easy way to sure endpoints at the lowest level.
This philosophy ensures that if a specific method `doAdminThings()` requires authorization, 
all methods that call `doAdminThings()` also require the same authorization.

Therefore,
you don't need to think about each method that is called at one endpoint to secure this endpoint from the top.
You can rely on your methods at the service level to do this for you.

### Require a Role

Let's say you have two endpoints: 
* `/api/cool-stuff` that should return cool stuff everybody should see.
* `/api/cool-stuff/for-admins` that only admins can see.

You fetch the cool stuff using `CoolStuff.forEveryone()` and `CoolStuff.forAdmins()`.
Therefore, you already know that the service method that provides the cool stuff for admins 
should already only be accessible for admins. 

You can implement it this way:

```kotlin
/**
 * This is a simple controller that returns cool stuff.
 */
@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val service: CoolStuffService
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
     * As you can see, no verification is required in the controller.
     */
    @GetMapping("for-admins")
    suspend fun getCoolStuffForAdmins(): ResponseEntity<CoolStuff> {
        return ResponseEntity.ok(service.getCoolStuffForAdmins)
    }
}

@Service
class CoolStuffService(
    private val authService: AuthorizationService
) {
    
    suspend fun getCoolStuff(): CoolStuff {
        return CoolStuff.forEveryone()
    }
    
    suspend fun getCoolStuffForAdmins(): CoolStuff {
        /**
         * Here's the catch!
         * You can use the `requireRole` method to specify which role a user should have to access this information.
         * It is already clear at this level that only admins should be able to see it.
         * Therefore, we specify it here already.
         */
        authService.requireRole(Roles.ADMIN)

        return CoolStuff.forAdmins()
    }
}

```

If you call `/api/cool-stuff/for-admins` now, you get:
* `200` if you are provided a valid token for a user with `ADMIN` role.
* `401` if you didn't provide a valid token.
* `403` if you provided a valid token but the user doesn't have the `ADMIN` role.

### Require Group Membership

You can be more fine-grained in your requirements.
Define and use custom groups and limit access to members of this group only.

:::note
Everything about creating and using groups is covered here: [Groups](/docs/usage/groups).
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
    private val service: CoolStuffService
) {

    /**
     * This endpoint is only accessible for users who are members of the group `cool-group`.
     * As you can see, no verification is required in the controller.
     */
    @GetMapping("for-cool-group")
    suspend fun getCoolStuffForCoolGroup(): ResponseEntity<CoolStuff> {
        return ResponseEntity.ok(service.getCoolStuffForCoolGroup)
    }
}

@Service
class CoolStuffService(
    private val authService: AuthorizationService
) {

    suspend fun getCoolStuffForCoolGroup(): CoolStuff {
        /**
         * Require the user to be a member of the group `cool-group`.
         */
        authService.requireGroupMemebership(GroupKeys.COOL_GROUP)

        return CoolStuff.forAdmins()
    }
}
```

If you call `/api/cool-stuff/for-cool-group` now, you get:
* `200` if you are provided a valid token for a user who is a member of `cool-group`.
* `401` if you didn't provide a valid token.
* `403` if you provided a valid token but the user is not a member of `cool-group`.

### Getting User Information

Let's say you have specific cool stuff prepared for each user using `CoolStuff.forUserWithId(..)`.

```kotlin
@RestController
@RequestMapping("/api/cool-stuff")
class CoolStuffController(
    private val service: CoolStuffService
) {

    /**
     * This endpoint provides specific information base on the user that is calling it.
     */
    @GetMapping
    suspend fun getCoolStuffForUserWithId(
        @PathVariable val id: ObjectId
    ): ResponseEntity<CoolStuff> {
        return ResponseEntity.ok(service.getCoolStuffForUserWithId(id))
    }
}

@Service
class CoolStuffService(
    private val authService: AuthorizationService
) {

    suspend fun getCoolStuffForUserWithId(): CoolStuff {
        /**
         * Get the user who called the endpoint from the security context.
         */
        val user = authService.getCurrentUser()

        return CoolStuff.forUserWithId(user.id)
    }
}
```

If you call `/api/cool-stuff` now, you get:
* `200` if you are provided a valid token for a user.
* `401` if you didn't provide a valid token.

#### Generic wildcard if no user token is provided

But what if you want to provide generic cool stuff if no user information is provided?

You can change the method slightly to do this:

```kotlin
@Service
class CoolStuffService(
    private val authService: AuthorizationService
) {

    suspend fun getCoolStuffForUserWithId(): CoolStuff {
        /**
         * Get the user who called the endpoint from the security context.
         */
        val user = authService.getCurrentUserOrNull()
        
        return if (user == null) {
            CoolStuff.forEveryone()
        } else {
            CoolStuff.forUserWithId(user.id)
        }
    }
}
```

If you call `/api/cool-stuff` now, you get:
* `200` whether you provide a token or not.

## Authorization By Path

If you decide to secure endpoints by path, you can define them using the following properties.

### Properties

| Property                        | Type           | Description                                                                | Default value |
|---------------------------------|----------------|----------------------------------------------------------------------------|---------------|
| `singularity.auth.public-paths` | `List<String>` | Paths that do not require authentication.                                  |               |
| `singularity.auth.user-paths`   | `List<String>` | Paths that require users to be authenticated and to have the `USER` role.  |               |
| `singularity.auth.admin-paths`  | `List<String>` | Paths that require users to be authenticated and to have the `ADMIN` role. |               |

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
      - /api/admin/path
```

## Header authentication

It is possible to authorize users using a bearer token in the request header. 
This option is enabled by default.

You can also configure if the access token stored inside an HTTP-only cookie or the bearer token should be preferred.

:::info
Please note that header authentication can be less secure because the tokens can be read from JavaScript.
This allows **XSS-attacks** if not configured properly.

HTTP-only cookies on the other hand are hidden from JavaScript and not directly accessible.
If you don't need header authentication, you can disable it here.
:::

### Properties

| Property                                        | Type      | Description                                                                                                                                           | Default value |
|-------------------------------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| `singularity.auth.allow-header-authentication`  | `Boolean` | Allow authentication using a bearer token placed in the header along HTTP-only Cookies. Allowed by default.                                           | `true`        |
| `singularity.auth.prefer-header-authentication` | `Boolean` | "If header authentication is allowed, this property controls the precedence: if true, the Authorization header is preferred over HTTP-only cookies.", | `true`        |


#### Example

```yaml
singularity:
  auth:
    allow-header-authentication: true
    prefer-header-authentication: true
```
