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

### 1. Retrieving a Session Token

Authentication in *Singularity* strongly relies on sessions. 
Access tokens and refresh tokens are bound to a specific session for example. 
If you try to perform a request with a session ID that does not belong to your current session, you will get an unauthorized response.

Therefore, before authenticating via an OAuth2 client, you need to retrieve a session token using [`GET /api/auth/sessions/token`](/swagger#/Sessions/generateTokenForCurrentSession).
This sets the session token as an HTTP-only cookie and returns the value in the response body if [header authentication](/docs/authorization/basics#header-authentication) is enabled.

### 2. Calling the Spring OAuth2 Authorization Endpoint

Spring automatically creates OAuth2 authorization endpoints for all of your clients on the path `/oauth2/authorization/{registrationId}`.

#### Parameters

| Parameter                          | Description                                                                                                                                                                                                                                                                                                                                                            | Required |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `redirect_uri`                     | The URI the user will be redirected to after the authentication was successful.                                                                                                                                                                                                                                                                                        | `false`  |
| `session_token`                    | The token specifying the current session you obtained from calling [`GET /api/auth/sessions/token`](/swagger#/Sessions/generateTokenForCurrentSession). It is not necessary to set this parameter since it will already be set as a HTTP-only cookie. You can override this value using this parameter or use it instead if for some reason cookies are not available. | `false`  |
| `oauth2_provider_connection_token` | A token used to connect a new OAuth2 client to an existing account. Go [here](#connecting-an-oauth2-client-to-an-existing-account) for more information.                                                                                                                                                                                                               | `false`  |

### 3. Redirect

If the authorization was successful, you will be redirected to `/login/oauth2/{registrationId}/code`.
*Singularity* will check the response and redirect the user to the `redirect_uri` if specified in the previous step.

The user is now authenticated.

## Connecting an OAuth2 Client to an Existing Account

It is possible to connect multiple OAuth2 clients to an account.

### 1. Authenticate the User

Log in the user and retrieve an `AccessToken`.

### 1. Creating an OAuth2 Provider Connection Token

Call [`POST /api/auth/providers/oauth2/token`](/swagger#/OAuth2%20Identity%20Provider/generateOAuth2ProviderConnectionToken) authenticated as the user to create an `OAuth2ProviderConnectionToken`.
This token will be set as an HTTP-only cookie and returned in the response if [header-authentication](/docs/authorization/basics#header-authentication) is enabled.

### 2. Follow the Steps For Registration

With the `OAuth2ProviderConnectionToken` set and the user authenticated, you can follow the same steps. 
As a result, the user will be connected to the new provider.

## Error Handling

If authentication failed, 
the user will be redirected to the URI you specify in `singularity.auth.oauth2.error-redirect-uri`.
The full URI will contain a query parameter `error` that specifies the type of error that occurred.
The following error types exist:

| Type                    | Description                                                                                                                         |
|-------------------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `email_exists`          | A user with the same email as in the email claim of the OAuth2 client is already registered. Therefore, no new user can be created. |
| `client_conntected`     | The user already connected another account of the same OAuth2 client. It is not possible to link another.                           |
| `invalid_token`         | If a token is invalid.                                                                                                              |
| `missing_session_token` | If the session token is not set.                                                                                                    |
| `server_error`          | An unspecified error occurred.                                                                                                      |
