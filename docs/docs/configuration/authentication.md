# Authentication

Authentication and authorization are core features of `Singularity`. You can configure how access token can be set and which paths should be secured.

:::info
All paths are public by default.
:::

There are two ways to secure your endpoints:
* You can either [define required roles by path](/docs/configuration/authorization-by-path)
* or [use the `Authentication Service`](/docs/usage/authentication#authorization) for authorization on the service level.

## Authorization By Path

If you decide to secure endpoints by path, you can define them using the following properties.

:::warning
The preferred way is by specifying the requirements in the service already.
Therefore, every endpoint that uses a service method that requires authorization will be secured by default.
It also allows setting more specific requirements such as group memberships.
:::

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
