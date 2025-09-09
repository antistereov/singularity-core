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
If [header authentication](/docs/authorization/basics#header-authentication) is enabled,
all tokens will be included in the response body of the corresponding request.
:::

## Configuration

| Property                                      | Type   | Description                                                                   | Default value |
|-----------------------------------------------|--------|-------------------------------------------------------------------------------|---------------|
| `singularity.security.jwt.expires-in`         | `Long` | Expiration time for JWT tokens in seconds. Default is 15 minutes.             | `900`         |
| `singularity.security.jwt.refresh-expires-in` | `Long` | Expiration time for refresh tokens in seconds. Default is about three months. | `7884000`     |


## Authentication

### Access Token

The **access token** is a user's personal ID.
It is only valid for a short amount of time
After successful login or registration this token will be set as HTTP-only cookie with this token will be set.

**You can retrieve this token from these endpoints:**
* [`POST /api/user/login`](/swagger#/User%20Session/login)
* [`POST /api/user/register`](/swagger#/User%20Session/register)

### Refresh Token

The **refresh token** can be used to retrieve a new access token once it is invalid.
They can be used once only.

Every single device a user logs in on has a unique refresh token.
Therefore, the user can revoke refresh tokens using the [device management](/docs/authorization/devices).

**You can retrieve this token from these endpoints:**
* [`POST /api/user/refresh`](/swagger#/User%20Session/refresh)

## Two-Factor

Two-factor authentication requires more specific tokens to control the flow and secure the application.
For more information about setup and usage of 2FA, check [this](/docs/authorization/two-factor).

### Two-Factor Setup Startup Token

Before setting up two-factor authentication, the user needs to enter his password.
If it was successful, the 

### Step-Up Token

### Login Verification Token

