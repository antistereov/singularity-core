---
sidebar_position: 4
description: Learn how to enable and configure two-factor authentication.
---

# Two-Factor Authentication

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

There are two methods to authenticate your users using a second factor: [TOTP](#totp) and [mail](#mail).


## TOTP


### Setup

#### 1. Step-Up


When successful, you will an HTTP-only cookie with a [`StepUpToken`](./tokens#step-up-token) is set.

If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled, you will get the token in the response body.

#### 2. Setup

Start the setup with [`GET /api/auth/2fa/setup`](/swagger#/TOTP%20Two-Factor%20Authentication/getTotpDetails).
You will receive your 2FA secret, a TOTP link and the recovery codes.

#### 3. Validate

Validate the setup with [`POST /api/auth/2fa/setup`](/swagger#/TOTP%20Two-Factor%20Authentication/setUpTotp).
You need to send the token and the correct code.
If the setup was successful, you will get the updated user information with 2FA enabled.
The cookie containing the `Two-Factor-Setup-Init-Token` will cleared as well.

The 2FA secret and the recovery codes are only saved inside the token.
Therefore, no problem occurs if the setup is canceled or not finished.
Every time you call [`GET /api/auth/2fa/setup`](/swagger#/User%20Session/register), a new 2FA secret and new recovery codes will be generated.

### Login

If 2FA is enabled, the login flow will be more complex.
After entering the correct credentials, you will get a `Two-Factor-Login-Token`.

Use this token to send a request to [`POST /api/auth/2fa/login`](/swagger#/Two%20Factor%20Authentication/verifyLogin).
If the token is valid and the code is correct, access token and refresh token will be set as HTTP-only cookies.
The user is logged in.

If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled, you will receive both access token and refresh token in the response body.

### Step-Up

### Disabling

2FA can be disabled using [`POST /api/auth/2fa/disable`](/swagger#/User%20Session/register) TODO.

### Recovery

If a user loses access to their 2FA session, they can use one of their recovery codes to recover their account.
This automatically sets a **Step-Up-Token**.
Therefore, the user can perform tasks that require step up authentication, e.g. disabling 2FA.

This can be done using [`POST /api/auth/2fa/recover`](/swagger#/User%20Session/register) TODO.

### Configuration

| Property                                              | Type      | Description                                                                                                                    | Default value |
|-------------------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------|---------------|
| singularity.auth.two-factor.totp.recovery-code.length | `Integer` | Length of the recovery code that can be used to log in if a user lost access to their second factor. Default is 10 characters. | `10`          |
| singularity.auth.two-factor.totp.recovery-code.count  | `Integer` | The number of recovery codes to generate. Every code can only be used once. Default is 6.                                      | `6`           |

## Mail

### Setup

### Login

### Step-Up

### Disabling

### Configuration

| Property                                         | Type      | Description                                                                                                                    | Default value |
|--------------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------|---------------|
| singularity.auth.two-factor.mail.code.expires-in | `Long`    | The number of seconds the 2FA code sent by email is valid.                                                                     | `900`         |


## Changing the Preferred Method


