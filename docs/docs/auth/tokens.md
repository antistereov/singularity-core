---
sidebar_position: 3
description: Understanding tokens.
---

# Tokens

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

Authentication in *Singularity* is based on [JSON Web Tokens](https://www.jwt.io/introduction).
These tokens include claims that will be used for authentication and authorization.
This way, the tokens are signed and can therefore verify their integrity.

:::info
If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled,
all tokens will be included in the response body of the corresponding request.
:::

## Access Token

The `AccessToken` serves as the user's **short-lived, digital identity**.

It's valid for a **short amount of time** (5 minutes by default) and contains important user information like ID, roles, and groups. 
This token is required for authentication with **every API request**.

### How to Obtain It

The token is generated and provided upon successful authentication, specifically through these endpoints:

* [`POST /api/auth/login`](/swagger#/Authentication/login): After a successful login.
* [`POST /api/auth/register`](/swagger#/Authentication/register): After a successful registration.
* [`POST /api/auth/refresh`](/swagger#/Authentication/refreshToken): When requesting a new token.

### Usage

The `AccessToken` can be attached to your API requests in two ways:

* **As an HTTP-only Cookie**: This is set and sent automatically by the browser.
    ```shell
    curl -X GET https://example.com/api/users/me \
      --cookie 'access_token=<your-token>'
    ```
* **As a Bearer Token in the Header**: This is an alternative if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.
    ```shell
    curl -X GET https://example.com/api/users/me \
      -H 'Authorization: Bearer <your-token>'
    ```

## Refresh Token

The `RefreshToken` is a **long-lived security token** whose sole purpose is 
to request a new [`AccessToken`](#access-token) and therefore extending the session 
without requiring the user to log in again.

It has a **long lifespan** (3 months by default) and is securely stored as an HTTP-only cookie. 
It serves as the *key* to renew the user's session if the [`AccessToken`](#access-token) has expired.

### How to Obtain It

The token is generated and provided upon successful authentication, specifically through these endpoints:

* [`POST /api/auth/login`](/swagger#/Authentication/login): After a successful login.
* [`POST /api/auth/register`](/swagger#/Authentication/register): After a successful registration.
* [`POST /api/auth/refresh`](/swagger#/Authentication/refreshToken): When requesting a new token.

### Usage

The `RefreshToken` is used to request a new `AccessToken`:

* **As an HTTP-only Cookie**: This is set and sent automatically by the browser.
    ```shell
    curl -X GET https://example.com/api/auth/refresh \
      --cookie 'refresh_token=<your-token>'
    ```
* **As a Bearer Token in the Header**: This is an alternative if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.
    ```shell
    curl -X GET https://example.com/api/auth/refresh \
      -H 'Authorization: Bearer <your-token>'
    ```

## Two-Factor Authentication Token

The `TwoFactorAuthenticationToken` is a temporary token used exclusively during the two-factor authentication process. 
It is set when a user with 2FA enabled attempts to log in or initiate a step-up authentication. 
Its purpose is to authorize the second step of this process.

It has a **short lifespan** and is used to verify the user's identity with their second factor (e.g., a one-time code). 
While this token is active, the standard [`AccessToken`](#access-token) and [`RefreshToken`](#refresh-token) are **not** generated, 
as the authentication flow is incomplete.

### How to Obtain It

This token is issued after a successful first-step authentication and is required to proceed with the second factor:

* [`POST /api/auth/login`](/swagger#/Authentication/login): When logging in to an account that has 2FA enabled.
* [`POST /api/auth/step-up`](/swagger#/Authentication/stepUp): When initiating a step-up authentication for an account that has 2FA enabled.

### Usage

The `TwoFactorAuthenticationToken` is used to complete the 2FA process by validating the second factor at the designated endpoints:

#### For Login

:::note
You can find more information about the login flow [here](./authentication#login).
:::

The  endpoint [`POST /api/auth/2fa/login`](/swagger#/Two%20Factor%20Authentication/verifyLogin) verifies the user's second factor and, 
if successful, issues the [`AccessToken`](#access-token) and [`RefreshToken`](#refresh-token).
* **As an HTTP-only Cookie**: This is set and sent automatically by the browser.
  ```shell
  curl -X POST https://example.com/api/auth/2fa/login \
    --cookie 'two_factor_authentication_token=<your-token>' \
    -H 'Content-Type: application/json' \
    -d <request-body>
  ```
* **In the Header**: This is an alternative if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.  
  ```shell
  curl -X POST https://example.com/api/auth/2fa/login \
    -H 'X-Two-Factor-Authentication-Token: <your-token>' \
    -H 'Content-Type: application/json' \
    -d <request-body>
  ```

#### For Step-Up Authentication

:::note
You can find more information about step-up flow [here](./authentication#step-up).
:::

The endpoint [`POST /api/auth/2fa/step-up`](/swagger#/Two%20Factor%20Authentication/verifyStepUp) verifies the second factor and, if successful, issues the [`StepUpToken`](#step-up-token) required for critical operations.
* **As an HTTP-only Cookie**: This is set and sent automatically by the browser.
    ```shell
    curl -X POST https://example.com/api/auth/2fa/step-up \
      --cookie 'two_factor_authentication_token=<your-token>' \
      -H 'Content-Type: application/json' \
      -d <request-body>
    ```
* **In the Header**: This is an alternative if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.
    ```shell
    curl -X POST https://example.com/api/auth/2fa/step-up \
    -H 'X-Two-Factor-Authentication-Token: <your-token>' \
    -H 'Content-Type: application/json' \
    -d <request-body>
    ```

## Step-Up Token

The `StepUpToken` is a temporary security token that authorizes a user to access **critical endpoints** 
after a successful reauthentication.
It acts as a second layer of security, 
signaling that the user has recently verified their identity again 
to perform sensitive actions like changing a password or deleting an account.

It has a **short lifespan** and is required in addition to a valid [`AccessToken`](#access-token) to access protected resources.

### How to Obtain It

:::note
You can find more information about the step-up flow [here](./authentication#step-up).
:::

This token is issued after a user successfully reauthenticates their session through the dedicated endpoint:

* [`POST /api/auth/step-up`](/swagger#/Authentication/stepUp): When reauthenticating a user to perform a sensitive action.

### Usage

The `StepUpToken` is required to authorize requests to sensitive API endpoints. It can be attached to your requests in two ways:

* **As an HTTP-only Cookie**: This is set and sent automatically by the browser.
    ```shell
    curl -X POST '[https://example.com/api/users/password](https://example.com/api/users/password)' \
      --cookie 'access_token=<your-access-token>; step_up_token=<your-step-up-token>'
    ```
* **In the Header**: If header authentication is enabled, you can provide it in the `X-Step-Up-Token` header.
    ```shell
    curl -X POST '[https://example.com/api/users/password](https://example.com/api/users/password)' \
      -H 'Authorization: Bearer <your-access-token>' \
      -H 'X-Step-Up-Token: <your-step-up-token>'
    ```

## Configuration

| Property                                    | Type   | Description                                                                   | Default value |
|---------------------------------------------|--------|-------------------------------------------------------------------------------|---------------|
| singularity.security.jwt.expires-in         | `Long` | Expiration time for JWT tokens in seconds. Default is 15 minutes.             | `900`         |
| singularity.security.jwt.refresh-expires-in | `Long` | Expiration time for refresh tokens in seconds. Default is about three months. | `7884000`     |


