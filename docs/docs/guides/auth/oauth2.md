---
sidebar_position: 6
description: Learn how to authenticate users using OAuth2 clients.
---

# OAuth2

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

To simplify the authentication process of your users, you can allow authentication using [OAuth2](https://auth0.com/intro-to-iam/what-is-oauth-2) clients.
This implementation is based on [Spring OAuth 2.0 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html#oauth2-client). Check out this guide if you need more information.

## Set Up an OAuth2 Provider

Create an application for your OAuth2 provider with the following parameters:

* `Redirect URI`: Use the base URI of your application (for example `https://example.com`), an identifier for the provider 
  (for example `github`) you configured for the client and the path `login/oauth2/<client-id>/code` 
  (for example `https://example.com/login/oauth2/github/code`)

Copy the `client-id` and `client-secret` for the next step.

## Configuration

[Spring OAuth 2.0](https://docs.spring.io/spring-security/reference/servlet/oauth2/client) provides an easy method to configure your OAuth2 providers.

:::warning
Please make sure that the **email** is in scope for your OAuth2 provider. 
Check out their official documentation.
:::

| Property                                   | Type      | Description                                                                       | Default value                             |
|--------------------------------------------|-----------|-----------------------------------------------------------------------------------|-------------------------------------------|
| singularity.auth.oauth2.enable             | `Boolean` | Allow authentication using OAuth2 identity providers. Disabled by default.        | `false`                                   |
| singularity.auth.oauth2.error-redirect-uri | `String`  | The path the user will be redirected to if there was an error in the OAuth2 flow. | `http://localhost:8000/auth/oauth2/error` |

Make sure to set `singularity.auth.oauth2.enable` to `true`.

### Clients

You can configure your clients with the following properties. 

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          okta: # registrationId
            client-id: <your-okta-client-id>
            client-secret: <your-okta-client-secret>
            authorization-grant-type: authorization_code
            scope: read, write
        provider:
          okta: # registrationId
            authorization-uri: https://<your-subdomain>.oktapreview.com/oauth2/v1/authorize
            token-uri: https://<your-subdomain>.oktapreview.com/oauth2/v1/token
```

For known providers like *GitHub* you don't need to specify the `provider` part:

```yaml
  security:
    oauth2:
      client:
        registration:
          github: # registrationId
            client-id: <your-github-client-id>
            client-secret: <your-github-client-id>
            scope:
              - user:email
              - read:user
```

For more information, check out the [Spring Docs](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorization-grants.html#oauth2-client-authorization-code).

## Registration and Login

If a user wants to register to your application 
or authenticate to an existing account using an OAuth2 provider, 
you have to perform the following steps:

:::info
This section strongly relies on **cookies**.
After successful authentication you will not be able to retrieve the `AccessToken` and `RefreshToken` though.
These tokens will be set as HTTP-only cookies only.

You only have the possibility to override certain tokens in the request header.
:::

### 1. Retrieving a Session Token

Authentication in *Singularity* strongly relies on sessions.
Access tokens and refresh tokens are bound to a specific session for example. 
You can learn more about that [here](./sessions).

Therefore, before authenticating via an OAuth2 client, 
you need to retrieve a [`SessionToken`](./tokens#session-token) using [`POST /api/auth/sessions/token`](../../api/generate-session-token.api.mdx).
This sets the [`SessionToken`](./tokens#session-token) as an HTTP-only cookie and returns the value in the response body 
if [header authentication](./authentication#header-authentication) is enabled.

### 2. Calling the Spring OAuth2 Authorization Endpoint

Spring automatically creates OAuth2 authorization endpoints for all of your providers 
on the path `/oauth2/authorization/{registrationId}`.

#### Parameters

| Parameter                          | Description                                                                                                                                                                                                                                                                                                                                                                                          | Required |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `redirect_uri`                     | The URI the user will be redirected to after the authentication was successful.                                                                                                                                                                                                                                                                                                                      | `false`  |
| `step_up`                          | Should step-up authentication be requested? Boolean. Learn more [here](#step-up-authentication).                                                                                                                                                                                                                                                                                                     | `false`  |

### 3. Redirect

If the authorization was successful, you will be redirected to `/login/oauth2/code/{registrationId}`.
*Singularity* will check the response and redirect the user to the `redirect_uri` if specified in the previous step.

The user is now authenticated.

## Step-Up Authentication

:::info
This section strongly relies on **cookies**.
Connecting a new provider to an existing user needs an [`AccessToken`](./tokens#access-token) set a cookie.
Placing them in the header will not lead to a successful connection since they will be lost after the callback.
:::

### 1. Authenticate the User

Authenticate the user by calling [`POST /api/auth/login`](../../api/login.api.mdx).
This will set the [`AccessToken`](./tokens#access-token) as an HTTP-only cookie.

### 2. Request the Step-Up

You can request a [`StepUpToken`](./tokens#step-up-token) by adding the parameter `step_up=true` to the initial authorization request 
(for example `https://example.com/oauth2/authorization/{registration_id}?step_up=true`).

This will set the [`StepUpToken`](./tokens#step-up-token) as an HTTP-only cookie.

You can learn more about step-up authentication [here](./authentication#step-up).

## Managing Providers

### Getting Connected Providers

You can request a list of connected providers using 
[`GET /api/users/me/providers`](../../api/get-identity-providers.api.mdx)
with a valid [`AccessToken`](./tokens#access-token).

### Connecting an OAuth2 Provider to an Existing Account

It is possible to connect multiple OAuth2 clients to an account.

:::info
This section strongly relies on **cookies**.
Connecting a new provider to an existing user needs an [`AccessToken`](./tokens#access-token) and a `StepUpToken` set as cookies.
Placing them in the header will not lead to a successful connection since they will be lost after the callback.
:::

#### 1. Authenticate the User

1. Make sure the user is authenticated and a valid [`AccessToken`](./tokens#access-token) is set as cookie.
2. Authorize a step-up by calling [`POST /api/auth/step-up`](../../api/step-up.api.mdx).
   This will set a [`StepUpToken`](./tokens#step-up-token) as an HTTP-only cookie.
   You can learn more about step-up authentication [here](./authentication#step-up).

#### 2. Create an OAuth2 Provider Connection Token

Call [`POST /api/users/me/providers/oauth2/token`](../../api/generate-o-auth-2-provider-connection-token.api.mdx) authenticated as the user to create an [`OAuth2ProviderConnectionToken`](./tokens#oauth2-provider-connection-token).
This token will be set as an HTTP-only cookie and returned in the response if [header-authentication](./authentication#header-authentication) is enabled.

#### 3. Follow the Steps For Registration

With the [`OAuth2ProviderConnectionToken`](./tokens#oauth2-provider-connection-token) set and the user authenticated, you can follow the same steps.
Because of the [`AccessToken`](./tokens#access-token), 
[`StepUpToken`](./tokens#step-up-token) and [`OAuth2ProviderConnectionToken`](./tokens#oauth2-provider-connection-token),
the server automatically tries to connect the new provider to the current user.

If successful, the user will be connected to the new provider.

:::info
A [security alert](./security-alerts.md#oauth2-specific-alerts)
will be sent to the user's email if this setting is enabled and
email is [enabled and configured correctly](../email/configuration.md).
:::

### Adding Password Authentication

If a user registered using an OAuth2 provider,
it is possible to add the option to authenticate using a password.

Call [`POST /api/users/me/providers/password`](../../api/add-password-authentication.api.mdx)
with a valid [`AccessToken`](./tokens#access-token) and [`StepUpToken`](./tokens#step-up-token).

If successful, the user can now log in using his new password.

### Disconnecting an OAuth2 Provider

If a user connected multiple provider,
it is possible to disconnect providers through the endpoint
[`DELETE /api/users/me/providers/<provider-name>`](../../api/delete-identity-provider.api.mdx).

:::info
A [security alert](./security-alerts.md#oauth2-specific-alerts)
will be sent to the user's email if this setting is enabled and
email is [enabled and configured correctly](../email/configuration.md).
:::

:::warning
You are not allowed to disconnect the password identity.
Furthermore, if the only identity is an OAuth2 identity, 
you are not allowed to disconnect this identity.
:::

## Error Handling

If authentication failed,
the user will be redirected to the URI you specify in `singularity.auth.oauth2.error-redirect-uri`.
This allows you to specifically handle these scenarios in your frontend.

The full URI will contain a query parameter `code` (for example `https://example.com/oauth2/error?code=state_parameter_missing`) that specifies the type of error that occurred.
The following error types exist:

### All Flows

These error codes can occur on any type of flow.

| Code                                 | Description                                                                                                                                                                                                                                                                                                             |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `authentication_failed`              | Authentication at OAuth2 provider failed. In this case another parameter `details` is included that specifies the error. This parameter correspond to error codes thrown by [Spring OAuth 2.0](https://docs.spring.io/spring-security/reference/servlet/oauth2/client). Check their documentation for more information. |
| `state_parameter_missing`            | No state parameter found in callback.                                                                                                                                                                                                                                                                                   |
| `state_expired`                      | The state token is expired. It is valid for 15 min by default.                                                                                                                                                                                                                                                          |
| `invalid_state`                      | The state token cannot be decoded.                                                                                                                                                                                                                                                                                      |
| `session_token_missing`              | No session token provided as query parameter or cookie.                                                                                                                                                                                                                                                                 |
| `session_token_expired`              | The provided session token is expired.                                                                                                                                                                                                                                                                                  |
| `invalid_session_token`              | The provided session token cannot be decoded.                                                                                                                                                                                                                                                                           |
| `sub_claim_missing`                  | No `sub` claim. provided from OAuth2 provider.                                                                                                                                                                                                                                                                          |
| `email_claim_missing`                | No `email` provided from OAuth2 provider.                                                                                                                                                                                                                                                                               |
| `server_error`                       | An unspecified error occurred.                                                                                                                                                                                                                                                                                          |

### Connection to Existing Account

Besides the error codes that can occur on all flows, 
these codes can occur when trying to connect an OAuth2 provider to an existing account.

| Code                                 | Description                                                                                                                         |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `provider_already_connected`         | The user already connected the provider.                                                                                            |
| `connection_token_expired`           | The provided [`OAuth2ProviderConnectionToken`](./tokens.md#oauth2-provider-connection-token) is expired.                            |
| `invalid_connection_token`           | The provided [`OAuth2ProviderConnectionToken`](./tokens.md#oauth2-provider-connection-token) cannot be decoded.                     |
| `connection_token_provider_mismatch` | The provided [`OAuth2ProviderConnectionToken`](./tokens.md#oauth2-provider-connection-token) does not match the requested provider. |
| `step_up_missing`                    | Connecting a new provider requires s [`StepUpToken`](./tokens.md#step-up-token).                                                    |
| `step_up_token_expired`              | The provided [`StepUpToken`](./tokens.md#step-up-token) is expired.                                                                 |
| `invalid_step_up_token`              | The provided [`StepUpToken`](./tokens.md#step-up-token) is invalid.                                                                 |
| `access_token_missing`               | Connecting a new provider requires an [`AccessToken`](./tokens#access-token).                                                       |
| `access_token_expired`               | The provided [`AccessToken`](./tokens#access-token) is expired.                                                                     |
| `invalid_access_token`               | The provided [`AccessToken`](./tokens#access-token) is invalid.                                                                     |

### Registration

Besides the error codes that can occur on all flows,
these codes can occur when trying to register a user via OAuth2.

| Code                         | Description                                                                                                                                                                                                                                                 |
|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `user_already_authenticated` | Registration failed. The user is already authenticated.                                                                                                                                                                                                     |
| `email_already_registered`   | Registration failed. The email attribute of the OAuth2 provider matches an email of an already registered user. In this case an [Identity Provider Information](./security-alerts.md#identity-provider-information) will be sent to the associated account. |


### Login

Besides the error codes that can occur on all flows,
these codes can occur when trying to log in a user account via OAuth2.

| Code                         | Description                                      |
|------------------------------|--------------------------------------------------|
| `user_already_authenticated` | Login failed. The user is already authenticated. |

### Step-Up

Besides the error codes that can occur on all flows,
these codes can occur when trying to perform [step-up authentication](./authentication.md#step-up) via OAuth2.

| Code                          | Description                                                                                     |
|-------------------------------|-------------------------------------------------------------------------------------------------|
| `access_token_missing`        | Connecting a new provider requires an [`AccessToken`](./tokens#access-token).                   |
| `access_token_expired`        | The provided [`AccessToken`](./tokens#access-token) is expired.                                 |
| `invalid_access_token`        | The provided [`AccessToken`](./tokens#access-token) is invalid.                                 |
| `wrong_account_authenticated` | The account you logged in via OAuth2 does not match the [`AccessToken`](./tokens#access-token). |

### Converting Guests to Users

| Code                                 | Description                                                                                                                                                   |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `user_already_authenticated`         | Conversion of [`GUEST`](../principals/introduction.md#guests) to user account failed. The user already authenticated.                                                            |
| `email_already_registered`           | Conversion of [`GUEST`](../principals/introduction.md#guests) to user account failed. The email attribute of the OAuth2 provider matches an email of an already registered user. |
| `provider_already_connected`         | The user already connected the provider.                                                                                                                      |
| `connection_token_expired`           | The provided [`OAuth2ProviderConnectionToken`](./tokens.md#oauth2-provider-connection-token) is expired.                                                      |
| `invalid_connection_token`           | The provided [`OAuth2ProviderConnectionToken`](./tokens.md#oauth2-provider-connection-token) cannot be decoded.                                               |
| `connection_token_provider_mismatch` | The provided [`OAuth2ProviderConnectionToken`](./tokens.md#oauth2-provider-connection-token) does not match the requested provider.                           |
| `step_up_missing`                    | Connecting a new provider requires step-up.                                                                                                                   |
| `step_up_token_expired`              | The provided [`StepUpToken`](./tokens.md#step-up-token) is expired.                                                                                           |
| `invalid_step_up_token`              | The provided [`StepUpToken`](./tokens.md#step-up-token) is invalid.                                                                                           |
