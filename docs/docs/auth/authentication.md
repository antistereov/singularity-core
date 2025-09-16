---
sidebar_position: 1
description: Learn how to register, log in or log out your users.
---

# Authentication

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

**Authentication** in *Singularity* relies on [Json Web Tokens (JWT)](https://www.jwt.io/introduction).
You can learn more about tokens in *Singularity* [here](../../docs/auth/tokens).

*Singularity* uses several different types of tokens to authenticate and authorize the user or to store information about the user.
Two particular tokens have a high relevance throughout the whole framework:

| Token                                                  | Lifespan                   | Usage                                                                                                                         |
|--------------------------------------------------------|----------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| [`AccessToken`](../../docs/auth/tokens#access-token)   | Short (5 min by default)   | It will be used in every request as HTTP-only cookie or in the authorization header as bearer token to authenticate the user. |
| [`RefreshToken`](../../docs/auth/tokens#refresh-token) | Long (3 months by default) | It's only purpose is to to request a new [`AccessToken`](../../docs/auth/tokens#access-token). Learn more [here](#refresh).   |

In summary, the short-lived [`AccessToken`](../../docs/auth/tokens#access-token) is the workhorse for every request, while the long-lived [`RefreshToken`](../../docs/auth/tokens#refresh-token) serves as a secure backup to renew the session without the user having to log in again.

Both tokens are generated on every successful login or register and automatically set as HTTP-only cookie.
If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled, it will also be returned in the response body.

## Example

:::info
After the user authenticated successfully (either through login or registration), 
both[`AccessToken`](../../docs/auth/tokens#access-token) and [`RefreshToken`](../../docs/auth/tokens#refresh-token) will be set as HTTP-only cookies.
If you perform another request from the same browser context, 
you don't need to explicitly set any tokens in your request.
:::

### Cookie

If you want to test or explicitly override the [`AccessToken`](../../docs/auth/tokens#access-token), you can send a request to your app like this:

```shell
curl -X GET 'https://example.com/api/users/me' \
  --cookie 'access_token=<your-access-token>'
```

### Header

If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled, 
you can set the access token as bearer token in the authorization header.

```shell
curl -X GET 'https://example.com/api/users/me' \
  -H 'Authorization: Bearer <your-access-token>'
```

## Registering Users

A new user can be registered by calling the endpoint [`POST /api/auth/register`](/swagger#/Authentication/register).
Users are uniquely identified by email address. 
An error response will be returned if a register request is send with an email that is already used.

After the registration succeeded, an HTTP-only cookie with [`AccessToken`](../../docs/auth/tokens#access-token) and [`RefreshToken`](../../docs/auth/tokens#refresh-token) will be set automatically.
If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled, access token and refresh token will be returned in the response body.

### Email Verification

If [mail is enabled](../mail/configuration), 
an email with a link to verify the email address will be sent to the user automatically after successful registration. 

The link will be generated based on the URI you configure here:

| Property                                | Type     | Description                                                                                                                  | Default value                             |
|-----------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| singularity.auth.email-verification.uri | `String` | The URI that will included in the verification email that leads to the email verification page in your frontend application. | `http://localhost:8000/auth/verify-email` |

This URI should lead to the email verification page of your frontend application.

This generated link will look like this: `<your-uri>?token=<generated-token>`.
For example, if you set the property to `https://example.com/auth/verify-email`, 
the generated link will look like this `https://example.com/auth/verify-email?token=ey27308dh7a7...`.

The link will include the query parameter `token` containing the token you need to verify the email.
Your frontend application should then request [`POST /api/auth/email/verify`](/swagger#/Email%20Verification/verifyEmail)
attaching the token.

If successful, the user's email address is verified.

#### Resending the Verification Email

You can resend the verification email using [`POST /api/auth/email/verify/send`](/swagger#/Email%20Verification/sendVerificationEmail).
After sending the email, a cooldown will be started.
The number of seconds the cooldown will take will be returned in the response body.

You are not allowed to send another email while the cooldown is active.
The cooldown can be configured [here](../mail/configuration).

You can check the state of the cooldown here [`GET /api/auth/email/verify/cooldown`](/swagger#/Email%20Verification/getRemainingVerificationCooldown).

### OAuth2

If [authentication via OAuth2 providers](../../docs/auth/oauth2) is enabled, 
users can also register using configured providers. You can find more information [here](../../docs/auth/oauth2#register-a-new-user).

## Login

You can log in a user by calling [`POST /api/auth/login`](/swagger#/Authentication/login).

After the login succeeded, an HTTP-only cookie with [`AccessToken`](../../docs/auth/tokens#access-token) and [`RefreshToken`](../../docs/auth/tokens#refresh-token) will be set automatically.
If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled, access token and refresh token will be returned in the response body.

### 2FA

If the user enabled [2FA](../../docs/auth/two-factor), 
a [`TwoFactorAuthenticationToken`](../../docs/auth/tokens#two-factor-authentication-token) will be set as an HTTP-only cookie and returned in the response body 
if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.
This token is necessary to perform the second step in the login process.

Depending on the [methods](../../docs/auth/two-factor#methods) the user configured, 
you can use the code you obtained from one of those methods to perform a second request to [`POST /api/auth/2fa/login`](/swagger#/Two%20Factor%20Authentication/verifyLogin).

If verification was successful, an HTTP-only cookie containing an [`AccessToken`](../../docs/auth/tokens#access-token) and one containing a [`RefreshToken`](../../docs/auth/tokens#refresh-token) will be set automatically.
If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled, access token and refresh token will be returned in the response body.
The [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token) will also be deleted.

### Password Reset

:::warning
You need to [enable mail](../mail/configuration).
Otherwise, the password reset will not be possible.
:::

You can request a password reset using [`POST /api/auth/password/reset-request`](/swagger#/Password%20Reset/sendPasswordResetEmail).
An email with a link to reset the password will be sent to the user.

The link will be generated based on the URI you configure here:

| Property                            | Type     | Description                                                                                                                   | Default value                               |
|-------------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| singularity.auth.password-reset.uri | `String` | The URI that will be included in the password reset email that leads to the password reset page in your frontend application. | `http://localhost:8000/auth/reset-password` |

This URI should lead to the password reset page of your frontend application.

This generated link will look like this: `<your-uri>?token=<generated-token>`.
For example, if you set the property to `https://example.com/auth/reset-password`,
the generated link will look like this `https://example.com/auth/reset-password?token=ey27308dh7a7...`.

The link will include the query parameter `token` containing the token you need to reset the password.
Your frontend application should then request [`POST /api/auth/password/reset](/swagger#/Password%20Reset/resetPassword)
attaching the token and specifying the new password.

If successful, the new password is set and the user can log in again.

:::info
All active sessions will be deleted after resetting the password.
:::

#### Resending the Reset Password Request

You can resend the password reset request using [`POST /api/auth/password/reset-request`](/swagger#/Password%20Reset/sendPasswordResetEmail).
After sending each email, a cooldown will be started.
The number of seconds the cooldown will take will be returned in the response body.

You are not allowed to send another email while the cooldown is active.
The cooldown can be configured [here](../mail/configuration).

You can check the state of the cooldown here [`GET /api/auth/password/reset/cooldown`](/swagger#/Password%20Reset/getRemainingPasswordResetCooldown).

### OAuth2

If [authentication via OAuth2 providers](../../docs/auth/oauth2) is enabled and the user [connected a provider](../../docs/auth/oauth2#connecting-an-oauth2-provider-to-an-existing-account),
users can log in using their connected providers. 
You can find more information [here](../../docs/auth/oauth2#register-a-new-user).

## Refresh

You can request a new [`AccessToken`](../../docs/auth/tokens#access-token) using [`POST /api/auth/refresh`](/swagger#/Authentication/refreshToken) 
using a valid [`RefreshToken`](../../docs/auth/tokens#refresh-token) as HTTP-only cookie `refresh_token` or as bearer token in the `Authorization` header if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.

If successful,  the [`RefreshToken`](../../docs/auth/tokens#refresh-token) you used becomes invalid, a new [`AccessToken`](../../docs/auth/tokens#access-token) and [`RefreshToken`](../../docs/auth/tokens#refresh-token) will be generated. 
Both tokens will be set as HTTP-only cookies and returned in the response body if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.


### Example

#### Cookie

If you want to test or explicitly override the [`RefreshToken`](../../docs/auth/tokens#refresh-token), you can send a request to your app like this:

```shell
curl -X GET 'https://example.com/api/auth/refresh' \
  --cookie 'refresh_token=<your-refresh-token>'
```

#### Header

If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled,
you can set the [`RefreshToken`](../../docs/auth/tokens#refresh-token) as bearer token in the authorization header.

```shell
curl -X GET 'https://example.com/api/auth/refresh' \
  -H 'Authorization: Bearer <your-refresh-token>'
```

## Logout

You can perform a logout using [`POST /api/auth/logout`](/swagger#/Authentication/logout).
Please note that you need to be **authenticated** to perform a logout.

If successful and authorized, the session will be deleted and all [`AccessToken`](../../docs/auth/tokens#access-token)s will be invalidated.

:::info
You can also delete all active sessions. You can find more information [here](../../docs/auth/sessions#invalidate-all-session).
:::

## Step-Up

*Singularity* allows you to secure critical endpoints with a second step.
Operations like changing the password or deleting the user account require reauthentication.
This means, an [`AccessToken`](../../docs/auth/tokens#access-token) is not enough to perform these requests. 
You need to provide a [`StepUpToken`](../../docs/auth/tokens.md#step-up-token) to authorize a step-up.

You can request a [`StepUpToken`](../../docs/auth/tokens.md#step-up-token) using [`POST /api/auth/step-up`](/swagger#/Authentication/stepUp).

:::info
Keep in mind that you also need to provide a valid [`AccessToken`](../../docs/auth/tokens#access-token).
Otherwise, the reauthentication will not be successful.
:::

If reauthenticated successfully, the [`StepUpToken`](../../docs/auth/tokens.md#step-up-token) will be set
as HTTP-only cookie and return in the response body if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.

### 2FA

If the user enabled [2FA](../../docs/auth/two-factor),
a [`TwoFactorAuthenticationToken`](../../docs/auth/tokens#two-factor-authentication-token) will be set as an HTTP-only cookie and returned in the response body
if [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled.
This token is necessary to perform the second step in the step-up process.

Depending on the [methods](../../docs/auth/two-factor#methods) the user configured,
you can use one of those methods to perform a second request to [`POST /api/auth/2fa/step-up`](/swagger#/Two%20Factor%20Authentication/verifyStepUp).

If verification was successful, an HTTP-only cookie containing your [`StepUpToken`](../../docs/auth/tokens.md#step-up-token) will be set automatically.
If [header authentication](../../docs/auth/securing-endpoints#header-authentication) is enabled, the [`StepUpToken`](../../docs/auth/tokens.md#step-up-token) will be returned in the response body.
The [`TwoFactorAuthenticationToken`](../../docs/auth/tokens#two-factor-authentication-token) will also be deleted.

### OAuth2

If [authentication via OAuth2 providers](../../docs/auth/oauth2) is enabled and the user [connected a provider](../../docs/auth/oauth2#connecting-an-oauth2-provider-to-an-existing-account),
users can request a step-up using their connected providers.
You can find more information [here](../../docs/auth/oauth2#step-up-authentication).

## Status

You can request the current authentication status using [`GET /api/auth/status`](/swagger#/Authentication/getStatus).
This will check the cookies and headers you set in the request.
