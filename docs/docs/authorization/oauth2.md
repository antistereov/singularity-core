---
sidebar_position: 5
description: Learn how to authenticate users using OAuth2 clients.
---

# OAuth2

:::info
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

To simplify the authentication process of your users, you can allow authentication using [OAuth2](https://auth0.com/intro-to-iam/what-is-oauth-2) clients.
This implementation is based on [Spring OAuth 2.0 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html#oauth2-client). Check out this guide if you need more information.

## Set Up an OAuth2 Client

Create an application for your OAuth2 client with the following parameters:

* `Redirect URI`: Use the base URI of your application (for example `https://example.com`), an identifier for the client (for example `github`) you configured for the client and the path `login/oauth2/<client-id>/code` (for example `https://example.com/login/oauth2/github/code`)

Copy the `client-id` and `client-secret` for the next step.

## Configuration

[Spring OAuth 2.0 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client) provides an easy method to configure your OAuth2 clients.

:::warning
Please make sure that the **email** is in scope for your OAuth2 client. Check out their official documentation.
:::

| Property                                     | Type      | Description                                                                       | Default value                             |
|----------------------------------------------|-----------|-----------------------------------------------------------------------------------|-------------------------------------------|
| `singularity.auth.oauth2.enable`             | `Boolean` | Allow authentication using OAuth2 identity providers. Disabled by default.        | `false`                                   |
| `singularity.auth.oauth2.error-redirect-uri` | `String`  | The path the user will be redirected to if there was an error in the OAuth2 flow. | `http://localhost:8000/auth/oauth2/error` |

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

For known clients like *GitHub* you don't need to specify the provider:

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

## Register a New User

If a user wants to register to your application using an OAuth2 client, you have to perform the following steps:

:::info
This section strongly relies on **cookies**.
After successful authentication you will not be able to retrieve the `AccessToken` and `RefreshToken` though.
These tokens will be set as HTTP-only cookies only.

You only have the possibility to override certain tokens in the request header.
:::

### 1. Retrieving a Session Token

Authentication in *Singularity* strongly relies on sessions.
Access tokens and refresh tokens are bound to a specific session for example. 
If you try to perform a request with a session ID that does not belong to your current session, you will get an unauthorized response.
You can learn more about that [here](/docs/authorization/sessions).

Therefore, before authenticating via an OAuth2 client, you need to retrieve a `SessionToken` using [`POST /api/auth/sessions/token`](/swagger#/Sessions/generateTokenForCurrentSession).
This sets the `SessionToken` as an HTTP-only cookie and returns the value in the response body if [header authentication](/docs/authorization/basics#header-authentication) is enabled.

### 2. Calling the Spring OAuth2 Authorization Endpoint

Spring automatically creates OAuth2 authorization endpoints for all of your clients on the path `/oauth2/authorization/{registrationId}`.

#### Parameters

| Parameter                          | Description                                                                                                                                                                                                                                                                                                                                                             | Required |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `redirect_uri`                     | The URI the user will be redirected to after the authentication was successful.                                                                                                                                                                                                                                                                                         | `false`  |
| `step_up`                          | Should step-up authentication be requested? Boolean. Learn more [here](#step-up-authentication).                                                                                                                                                                                                                                                                        | `false`  |
| `session_token`                    | The token specifying the current session you obtained from calling [`POST /api/auth/sessions/token`](/swagger#/Sessions/generateTokenForCurrentSession). It is not necessary to set this parameter since it will already be set as a HTTP-only cookie. You can override this value using this parameter or use it instead if for some reason cookies are not available. | `false`  |
| `oauth2_provider_connection_token` | A token used to connect a new OAuth2 client to an existing account. It is not necessary to set this parameter since this token will already be set as an HTTP-only cookie. You can use this parameter to override the token. Go [here](#connecting-an-oauth2-client-to-an-existing-account) for more information.                                                       | `false`  |

### 3. Redirect

If the authorization was successful, you will be redirected to `/login/oauth2/{registrationId}/code`.
*Singularity* will check the response and redirect the user to the `redirect_uri` if specified in the previous step.

The user is now authenticated.

## Connecting an OAuth2 Client to an Existing Account

It is possible to connect multiple OAuth2 clients to an account.

:::info
This section strongly relies on **cookies**.
Connecting a new provider to an existing user needs an `AccessToken` and a `StepUpToken` set as cookies.
Placing them in the header will not lead to a successful connection since they will be lost after the callback.
:::

### 1. Authorizing the User

1. Log in the user by calling [`POST /api/auth/login`](/swagger#/Authentication/login).
    This will set the `AccessToken` as an HTTP-only cookie.
2. Authorize a step-up by calling [POST /api/auth/step-up](/swagger#/Authentication/stepUp).
    This will set a `StepUpToken` as an HTTP-only cookie.

### 2. Creating an OAuth2 Provider Connection Token

Call [`POST /api/auth/providers/oauth2/token`](/swagger#/OAuth2%20Identity%20Provider/generateOAuth2ProviderConnectionToken) authenticated as the user to create an `OAuth2ProviderConnectionToken`.
This token will be set as an HTTP-only cookie and returned in the response if [header-authentication](/docs/authorization/basics#header-authentication) is enabled.

### 3. Follow the Steps For Registration

With the `OAuth2ProviderConnectionToken` set and the user authenticated, you can follow the same steps.
Because of the `AccessToken`, `StepUpToken` and `OAuth2ProviderConnectionToken`, 
the server automatically tries to connect the new provider to the current user.

If successful, the user will be connected to the new provider.

## Step-Up Authentication

:::info
This section strongly relies on **cookies**.
Connecting a new provider to an existing user needs an `AccessToken` set a cookie.
Placing them in the header will not lead to a successful connection since they will be lost after the callback.
:::

### 1. Authorize the User

Log in the user by calling [`POST /api/auth/login`](/swagger#/Authentication/login).
This will set the `AccessToken` as an HTTP-only cookie.

### 2. Requesting the Step-Up

You can request a `StepUpToken` by adding the parameter `step_up=true` to the initial authorization request 
(for example `https://example.com/oauth2/authorization/{registration_id}?step_up=true`).

This will set the `StepUpToken` as an HTTP-only cookie.

You can learn more about step-up authentication [here](/docs/authorization/authentication-flow#step-up).

## Error Handling

If authentication failed, 
the user will be redirected to the URI you specify in `singularity.auth.oauth2.error-redirect-uri`.
This allows you to specifically handle these scenarios in your frontend.

The full URI will contain a query parameter `error` (for example `https://example.com/oauth2/error?error=state_parameter_missing`) that specifies the type of error that occurred.
The following error types exist:

| Code                                 | Description                                                                                                                   |
|--------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `state_parameter_missing`            | No state parameter found in callback.                                                                                         |
| `user_already_authenticated`         | Login with an existing account connected to the provider failed because the user already authenticated.                       |
| `session_token_missing`              | No session token provided as query parameter or cookie.                                                                       |
| `session_token_expired`              | The provided session token is expired.                                                                                        |
| `invalid_session_token`              | The provided session token cannot be decoded.                                                                                 |
| `principal_id_missing`               | No principal ID provided from OAuth2 provider.                                                                                |
| `email_attribute_missing`            | No email provided from OAuth2 provider.                                                                                       |
| `connection_token_missing`           | Failed to connect a new provider to the current user. No OAuth2ProviderConnection set as cookie or sent as request parameter. |
| `provider_already_connected`         | The user already connected the provider.                                                                                      |
| `connection_token_expired`           | The provided OAuth2ProviderConnectionToken is expired.                                                                        |
| `invalid_connection_token`           | The provided OAuth2ProviderConnectionToken cannot be decoded.                                                                 |
| `connection_token_provider_mismatch` | The provided OAuth2ProviderConnectionToken does not match the requested provider.                                             |
| `server_error`                       | An unspecified error occurred.                                                                                                |
