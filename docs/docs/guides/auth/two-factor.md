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

:::warning
Two-factor authentication can only be used for accounts 
that configured authentication using a password.

When using an [OAuth2 provider](./oauth2), authentication and authorization
is controlled by the provider.
:::

## TOTP

TOTP (Time-based One-time Password) is a two-factor authentication method 
that generates a **temporary, unique, and time-sensitive** password. 
The code is created using a shared secret key and the current time, 
making it valid for only a short period (typically 30-60 seconds). 
It's a common method for securing online accounts. 

It is also commonly known as **Authenticator App** or **Authentication App**, 
as it's frequently implemented through dedicated applications 
like Google Authenticator or Microsoft Authenticator.

### Setup

:::info
When adding a new 2FA method, all active sessions will be deleted for security reasons.
:::

#### 1. Step-Up

Enabling a new 2FA method is a critical setting. 
Therefore, a [`StepUpToken`](./tokens#step-up-token) is required.

#### 2. Setup

Start the setup with [`GET /api/auth/2fa/setup`](/swagger#/TOTP%20Two-Factor%20Authentication/getTotpDetails).
You will receive your 2FA secret, a TOTP link and the recovery codes and a token.

The 2FA secret and the recovery codes are only saved inside the token.
Therefore, no problem occurs if the setup is canceled or not finished.
Every time you call [`GET /api/auth/2fa/setup`](/swagger#/User%20Session/register), a new 2FA secret and new recovery codes will be generated.

#### 3. Validate

Validate the setup with [`POST /api/auth/2fa/setup`](/swagger#/TOTP%20Two-Factor%20Authentication/setUpTotp).
You need to send the token and the correct TOTP code.
If the setup was successful, you will get the updated user information with 2FA enabled.

:::note
When the setup was successful,
TOTP will be configured as preferred 2FA method.
:::

### Login

:::note
Learn more about the login flow [here](./authentication#login).
:::

#### 1. Authenticate with Password

When TOTP is enabled,
a successful login with email and password through [`POST /api/auth/login`](/swagger#/Authentication/login) 
will set a [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token)
as an HTTP-only cookie and returned in the response body if [header authentication](/securing-endpoints#header-authentication) is enabled.

#### 2. Authenticate with TOTP

Use this token to send a request to [`POST /api/auth/2fa/login`](/swagger#/Two%20Factor%20Authentication/verifyLogin).
Enter the TOTP code with the parameter `totp` in the request body.

If the token is valid and the code is correct, [`AccessToken`](./tokens#access-token) 
and [`RefreshToken`](./tokens#refresh-token) will be set as HTTP-only cookies.
The user is successfully authenticated.
If [header authentication](/securing-endpoints#header-authentication) is enabled, 
you will receive both tokens in the response body.

### Step-Up

:::note
Learn more about the step-up flow [here](./authentication#step-up).
:::

#### 1. Authenticate with Password

When TOTP is enabled,
a successful step-up request with email and password through [`POST /api/auth/step-up`](/swagger#/Authentication/stepUp) 
will set a [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token)
as an HTTP-only cookie and returned in the response body if [header authentication](/securing-endpoints#header-authentication) is enabled.

#### 2. Authenticate with TOTP

Use the [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token) together with the user's [`AccessToken`](./tokens#access-token) to send a request to [`POST /api/auth/2fa/step-up`](/swagger#/Two%20Factor%20Authentication/verifyStepUp).
Enter the TOTP code with the parameter `totp` in the request body.

If the [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token) is valid and the code is correct, [`AccessToken`](./tokens#access-token)
and [`RefreshToken`](./tokens#refresh-token) will be set as HTTP-only cookies.
The user is successfully authenticated.
If [header authentication](/securing-endpoints#header-authentication) is enabled,
you will receive both tokens in the response body.

### Disable

:::warning
If TOTP is the only enabled 2FA method, you cannot disable it.
For security reasons it is required to at least enable one 2FA method.
:::

Disabling TOTP is a critical setting, therefore a [`StepUpToken`](./tokens#step-up-token) is required.

TOTP can be disabled through the endpoint [`DELETE /api/auth/2fa/totp`](/swagger#/TOTP%20Two-Factor%20Authentication/disableTwoFactorAuth)
using a valid [`AccessToken`](./tokens#access-token) and [`StepUpToken`](./tokens#step-up-token).

### Recovery

If a user loses access to their TOTP device, they can use one of their **recovery codes** to recover their account
through the endpoint [`POST /api/auth/2fa/recover`](/swagger#/TOTP%20Two-Factor%20Authentication/recoverUser).

:::info
Each recovery code is valid once.
:::

This automatically sets an [`AccessToken`](./tokens#access-token),
and [`RefreshToken`](./tokens#refresh-token) and a [`StepUpToken`](./tokens#step-up-token).
Therefore, the user can perform tasks that require step-up authentication, 
such as **disabling TOTP**.

### Configuration

| Property                                              | Type      | Description                                                                                                                    | Default value |
|-------------------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------|---------------|
| singularity.auth.two-factor.totp.recovery-code.length | `Integer` | Length of the recovery code that can be used to log in if a user lost access to their second factor. Default is 10 characters. | `10`          |
| singularity.auth.two-factor.totp.recovery-code.count  | `Integer` | The number of recovery codes to generate. Every code can only be used once. Default is 6.                                      | `6`           |

## Mail

:::info
If [mail is enabled](../mail/configuration) in your application,
mail as a 2FA method will be automatically enabled for every user that registers using a password.
:::

Users can use mail 2FA codes that will be sent to the users' email address.
These codes are valid for short amount of time (15 min by default).
The expiration can be configured [here](#configuration-1).

### Sending a 2FA Code via Email

You can send an email containing a 2FA code through the endpoint
[`POST /api/auth/2fa/mail/send`](/swagger#/Mail%20Two-Factor%20Authentication/sendAuthenticationMail).

After sending the email, a cooldown will be started.
The number of seconds the cooldown will take will be returned in the response body.

:::warning
Each request will generate a new code and invalidate all old codes.
:::

You are not allowed to send another email while the cooldown is active.
The cooldown can be configured [here](../mail/configuration).

:::note
You can check the state of the cooldown here [`GET /api/auth/2fa/mail/cooldown`](/swagger#/Mail%20Two-Factor%20Authentication/getRemainingMailTwoFactorCooldown).
:::

### Setup

:::info
When adding a new 2FA method, all active sessions will be deleted for security reasons.
:::

#### 1. Sending an Email with the 2FA Code

Request a new 2FA code following [this](#sending-a-2fa-code-via-email) guide.

#### 2. Step-Up

Enabling a new 2FA method is a critical setting.
Therefore, a [`StepUpToken`](./tokens#step-up-token) is required.

#### 3. Validate

You can validate and enable mail as a 2FA method through the endpoint
[`/api/auth/2fa/mail/enable`](/swagger#/Mail%20Two-Factor%20Authentication/enableMail)
using your [`AccessToken`](./tokens#access-token) and [`StepUpToken`](./tokens#step-up-token).

### Login

:::note
Learn more about the login flow [here](./authentication#login).
:::

#### 1. Authenticate with Password

When mail as 2FA method is enabled,
a successful login with email and password through [`POST /api/auth/login`](/swagger#/Authentication/login)
will set a [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token)
as an HTTP-only cookie and returned in the response body if [header authentication](/securing-endpoints#header-authentication) is enabled.

#### 2. Sending an Email with the 2FA Code

:::info
If mail is the preferred 2FA method, an email with the 2FA code will be automatically 
sent after successful authentication with password.
You can learn how to change the preferred method [here](#changing-the-preferred-method).
:::

Request a new 2FA code following [this](#sending-a-2fa-code-via-email) guide.

#### 3. Authenticate with TOTP

Use this token to send a request to [`POST /api/auth/2fa/login`](/swagger#/Two%20Factor%20Authentication/verifyLogin).
Enter the 2FA code with the parameter `mail` in the request body.

If the token is valid and the code is correct, [`AccessToken`](./tokens#access-token)
and [`RefreshToken`](./tokens#refresh-token) will be set as HTTP-only cookies.
The user is successfully authenticated.
If [header authentication](/securing-endpoints#header-authentication) is enabled,
you will receive both tokens in the response body.

### Step-Up

:::note
Learn more about the login flow [here](./authentication#login).
:::

#### 1. Authenticate with Password

When mail as 2FA method is enabled,
a successful step-up with email and password through [`POST /api/auth/step-up`](/swagger#/Authentication/stepUp)
will set a [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token)
as an HTTP-only cookie and returned in the response body if [header authentication](/securing-endpoints#header-authentication) is enabled.

#### 2. Sending an Email with the 2FA Code

:::info
If mail is the preferred 2FA method, an email with the 2FA code will be automatically
sent after successful authentication with password.
You can learn how to change the preferred method [here](#changing-the-preferred-method).
:::

Request a new 2FA code following [this](#sending-a-2fa-code-via-email) guide.

#### 3. Authenticate with TOTP

Use this token to send a request to [`POST /api/auth/2fa/step-up`](/swagger#/Two%20Factor%20Authentication/verifyStepUp).
Enter the 2FA code with the parameter `mail` in the request body.

If the token is valid and the code is correct, [`AccessToken`](./tokens#access-token)
and [`RefreshToken`](./tokens#refresh-token) will be set as HTTP-only cookies.
The user is successfully authenticated.
If [header authentication](/securing-endpoints#header-authentication) is enabled,
you will receive both tokens in the response body.

### Disable

:::warning
If mail is the only enabled 2FA method, you cannot disable it.
For security reasons it is required to at least enable one 2FA method.
:::

Disabling mail as a 2FA method is a critical setting, therefore a [`StepUpToken`](./tokens#step-up-token) is required.

TOTP can be disabled through the endpoint [`DELETE /api/auth/2fa/mail`](/swagger#/Mail%20Two-Factor%20Authentication/disableMail)
using a valid [`AccessToken`](./tokens#access-token) and [`StepUpToken`](./tokens#step-up-token).


### Configuration

| Property                                         | Type      | Description                                                                                                                    | Default value |
|--------------------------------------------------|-----------|--------------------------------------------------------------------------------------------------------------------------------|---------------|
| singularity.auth.two-factor.mail.code.expires-in | `Long`    | The number of seconds the 2FA code sent by email is valid.                                                                     | `900`         |


## Changing the Preferred Method

You can change the preferred method through the endpoint [`POST /api/auth/2fa/preferred-method`](/swagger#/Two%20Factor%20Authentication/updatePreferredMethod)
using a valid [`AccessToken`](./tokens#access-token) and [`StepUpToken`](./tokens#step-up-token).

The current preferred method can be requested through [`GET /api/auth/status`](/swagger#/Authentication/getStatus).
