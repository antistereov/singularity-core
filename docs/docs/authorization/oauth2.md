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

* `Redirect URI`: Use your base URI (for example `https://example.com`), an identifier for the client (for example `github`) you configured for the client and the path `login/oauth2/<client-id>/code` (for example `https://example.com/login/oauth2/github/code`)

Copy the

## Configuration

[Spring OAuth 2.0 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client) provides an easy method to configure your OAuth2 clients.

:::warning
Please make sure that the **email** is in scope for your OAuth2 client. Check out their official documentation.
:::

At first, you need to enable authentication via OAuth2 clients:

```yaml
singularity:
  auth:
    allow-oauth2-providers: true
```

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

| Parameter       | Description                                                                                                                                                                                                                                                                                                                                                            | Required |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `redirect_uri`  | The URI the user will be redirected to after the authentication was successful.                                                                                                                                                                                                                                                                                        | `false`  |
| `session_token` | The token specifying the current session you obtained from calling [`GET /api/auth/sessions/token`](/swagger#/Sessions/generateTokenForCurrentSession). It is not necessary to set this parameter since it will already be set as a HTTP-only cookie. You can override this value using this parameter or use it instead if for some reason cookies are not available. | `false`  |

### 3. Redirect

If the authorization was successful, you will be redirected to `/login/oauth2/{registrationId}/code`.
*Singularity* will check the response and redirect the user to the `redirect_uri` if specified in the previous step.

The user is now authenticated.

#### Errors

The check can fail in the following cases:

* The session token is expired. Perform the authorization again with a new token.
* A user with the same e-mail address is already registered. In case that the users wants to connect the OAuth2 client to his account, you can follow [this](#connecting-an-oauth2-client-to-an-existing-account) guide.

## Connecting an OAuth2 Client to an Existing Account
