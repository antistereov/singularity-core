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
You can learn more about tokens in *Singularity* [here](./tokens).

*Singularity* uses several different types of tokens to authenticate and authorize the user or to store information about the user.
Two particular tokens have a high relevance throughout the whole framework:

| Token                                    | Lifespan                   | Usage                                                                                                                         |
|------------------------------------------|----------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| [`AccessToken`](./tokens#access-token)   | Short (5 min by default)   | It will be used in every request as HTTP-only cookie or in the authorization header as bearer token to authenticate the user. |
| [`RefreshToken`](./tokens#refresh-token) | Long (3 months by default) | It's only purpose is to to request a new [`AccessToken`](./tokens#access-token). Learn more [here](#refresh).                 |

In summary, the short-lived [`AccessToken`](./tokens#access-token) is the workhorse for every request,
while the long-lived [`RefreshToken`](./tokens#refresh-token) serves as a secure backup to renew the session without the user having to log in again.

Both tokens are generated on every successful login or register and automatically set as HTTP-only cookie.
If [header authentication](./authentication#header-authentication) is enabled,
it will also be returned in the response body.

## Example

:::info
After the user authenticated successfully (either through login or registration), 
both[`AccessToken`](./tokens#access-token) and [`RefreshToken`](./tokens#refresh-token) will be set as HTTP-only cookies.
If you perform another request from the same browser context, 
you don't need to explicitly set any tokens in your request.
:::

### Cookie

If you want to test or explicitly override the [`AccessToken`](./tokens#access-token), 
you can send a request to your app like this:

```shell
curl -X GET 'https://example.com/api/users/me' \
  --cookie 'access_token=<your-access-token>'
```

### Header

If [header authentication](./authentication#header-authentication) is enabled, 
you can set the access token as bearer token in the authorization header.

```shell
curl -X GET 'https://example.com/api/users/me' \
  -H 'Authorization: Bearer <your-access-token>'
```

## Registering Users

A new user can be registered by calling the endpoint [`POST /api/auth/register`](../../api/register.api.mdx).
Users are uniquely identified by email address. 

If successful and if the email is not linked to any existing account, 
the user will receive an email with a link to verify the email address.

In case that email is already connected to an existing account, a [warning](./security-alerts.md#core-identity-alerts) will be sent to the
corresponding email address informing the user.

:::info Security
To prevent account enumeration attacks, this endpoint will return response code `201`
no matter if a new account is created or not.
:::

### Email Verification

If [email is enabled](../email/configuration), 
an email with a link to verify the email address will be sent to the user automatically after successful registration. 

The link will be generated based on the URI you configure here:

| Property                              | Type     | Description                                                                                                                  | Default value                             |
|---------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| singularity.ui.email-verification-uri | `String` | The URI that will included in the verification email that leads to the email verification page in your frontend application. | `http://localhost:4200/auth/verify-email` |

This URI should lead to the email verification page of your frontend application.

This generated link will look like this: `<your-uri>?token=<generated-token>`.
For example, if you set the property to `https://example.com/auth/verify-email`, 
the generated link will look like this `https://example.com/auth/verify-email?token=ey27308dh7a7...`.

The link will include the query parameter `token` containing the token you need to verify the email.
Your frontend application should then request [`POST /api/auth/email/verification`](../../api/verify-email.api.mdx)
attaching the token.

If successful, the user's email address is verified.

:::note
If there is no account associated with the given email address,
a [No Account Information](./security-alerts#no-account-information)
email will be sent to the given email address.
:::

#### Resending the Verification Email

You can resend the verification email using [`POST /api/auth/email/verification/send`](../../api/send-email-verification-email.api.mdx).
After sending the email, a cooldown will be started.
The number of seconds the cooldown will take will be returned in the response body.

You are not allowed to send another email while the cooldown is active.
The cooldown can be configured [here](../email/configuration).

You can check the state of the cooldown here [`GET /api/auth/email/verification/cooldown`](../../api/get-remaining-email-verification-cooldown.api.mdx).

### OAuth2

If [authentication via OAuth2 providers](./oauth2) is enabled, 
users can also register using configured providers. 
You can find more information [here](./oauth2#registration-and-login).

## Login

You can log in a user by calling [`POST /api/auth/login`](../../api/login.api.mdx).

After the login succeeded,
an HTTP-only cookie with [`AccessToken`](./tokens#access-token) and [`RefreshToken`](./tokens#refresh-token) will be set automatically.
If [header authentication](./authentication#header-authentication) is enabled,
access token and refresh token will be returned in the response body.

:::info
A [login alert](./security-alerts.md#core-identity-alerts)
will be sent to the user's email if this setting is enabled and 
email is [enabled and configured correctly](../email/configuration.md).
:::

### 2FA

If the user enabled [2FA](./two-factor), 
a [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token) will be set as an HTTP-only cookie and returned in the response body 
if [header authentication](./authentication#header-authentication) is enabled.
This token is necessary to perform the second step in the login process.

Depending on the [methods](./two-factor) the user configured, 
you can use the code you obtained from one of those methods to perform a second request to [`POST /api/auth/2fa/login`](../../api/complete-login.api.mdx).

If verification was successful, 
an HTTP-only cookie containing an [`AccessToken`](./tokens#access-token) and one containing a [`RefreshToken`](./tokens#refresh-token) will be set automatically.
If [header authentication](./authentication#header-authentication) is enabled,
access token and refresh token will be returned in the response body.
The [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token) will also be deleted.

### Password Reset

:::warning
You need to [enable mail](../email/configuration).
Otherwise, the password reset will not be possible.
:::

You can request a password reset using [`POST /api/auth/password/reset-request`](../../api/send-password-reset-email.api.mdx).
An email with a link to reset the password will be sent to the user.

The link will be generated based on the URI you configure here:

| Property                          | Type     | Description                                                                                                                   | Default value                               |
|-----------------------------------|----------|-------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------|
| singularity.ui.password-reset-uri | `String` | The URI that will be included in the password reset email that leads to the password reset page in your frontend application. | `http://localhost:4200/auth/reset-password` |

This URI should lead to the password reset page of your frontend application.

This generated link will look like this: `<your-uri>?token=<generated-token>`.
For example, if you set the property to `https://example.com/auth/reset-password`,
the generated link will look like this `https://example.com/auth/reset-password?token=ey27308dh7a7...`.

The link will include the query parameter `token` containing the token you need to reset the password.
Your frontend application should then request [`POST /api/auth/password/reset](../../api/reset-password.api.mdx)
attaching the token and specifying the new password.

If successful, the new password is set and the user can log in again.

:::info
All active sessions will be deleted after resetting the password.
:::

:::note
If there is no account associated with the given email address,
a [No Account Information](./security-alerts#no-account-information)
email will be sent to the given email address.
:::

#### Resending the Reset Password Request

You can resend the password reset request using [`POST /api/auth/password/reset-request`](../../api/send-password-reset-email.api.mdx).
After sending each email, a cooldown will be started.
The number of seconds the cooldown will take will be returned in the response body.

You are not allowed to send another email while the cooldown is active.
The cooldown can be configured [here](../email/configuration).

You can check the state of the cooldown here [`GET /api/auth/password/reset/cooldown`](../../api/get-remaining-password-reset-cooldown.api.mdx).

### OAuth2

If [authentication via OAuth2 providers](./oauth2) is enabled and the user [connected a provider](./oauth2#connecting-an-oauth2-provider-to-an-existing-account),
users can log in using their connected providers. 
You can find more information [here](./oauth2#registration-and-login).

## Refresh

You can request a new [`AccessToken`](./tokens#access-token) using [`POST /api/auth/refresh`](../../api/refresh-access-token.api.mdx) 
using a valid [`RefreshToken`](./tokens#refresh-token) as HTTP-only cookie `refresh_token` or as bearer token in the `Authorization` header if [header authentication](./authentication#header-authentication) is enabled.

If successful,  the [`RefreshToken`](./tokens#refresh-token) you used becomes invalid, 
a new [`AccessToken`](./tokens#access-token) and [`RefreshToken`](./tokens#refresh-token) will be generated. 
Both tokens will be set as HTTP-only cookies and returned in the response body if [header authentication](./authentication#header-authentication) is enabled.


### Example

#### Cookie

If you want to test or explicitly override the [`RefreshToken`](./tokens#refresh-token), 
you can send a request to your app like this:

```shell
curl -X GET 'https://example.com/api/auth/refresh' \
  --cookie 'refresh_token=<your-refresh-token>'
```

#### Header

If [header authentication](./authentication#header-authentication) is enabled,
you can set the [`RefreshToken`](./tokens#refresh-token) as bearer token in the authorization header.

```shell
curl -X GET 'https://example.com/api/auth/refresh' \
  -H 'Authorization: Bearer <your-refresh-token>'
```

## Logout

You can perform a logout using [`POST /api/auth/logout`](../../api/logout.api.mdx).

If the user was authorized, 
the session will be deleted and all [`AccessToken`](./tokens#access-token)s will be invalidated.

All cookies related to *Singularity* will be deleted.

:::info
You can also delete all active sessions.
You can find more information [here](./sessions#invalidating-all-session).
:::

## Step-Up

*Singularity* allows you to secure critical endpoints with a second step.
Operations like changing the password or deleting the user account require reauthentication.
This means, an [`AccessToken`](./tokens#access-token) is not enough to perform these requests. 
You need to provide a [`StepUpToken`](./tokens#step-up-token) to authorize a step-up.

:::note
You can learn more on how to secure your endpoints by requiring a step-up [here](./securing-endpoints#3-requiring-roles-and-group-membership)
:::

You can request a [`StepUpToken`](./tokens#step-up-token) using [`POST /api/auth/step-up`](../../api/step-up.api.mdx).

:::info
Keep in mind that you also need to provide a valid [`AccessToken`](./tokens#access-token).
Otherwise, the reauthentication will not be successful.
:::

If reauthenticated successfully, the [`StepUpToken`](./tokens#step-up-token) will be set
as HTTP-only cookie and return in the response body if [header authentication](./authentication#header-authentication) is enabled.

### 2FA

If the user enabled [2FA](./two-factor),
a [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token) will be set as an HTTP-only cookie and returned in the response body
if [header authentication](./authentication#header-authentication) is enabled.
This token is necessary to perform the second step in the step-up process.

Depending on the [methods](./two-factor) the user configured,
you can use one of those methods to perform a second request to [`POST /api/auth/2fa/step-up`](../../api/complete-step-up.api.mdx).

If verification was successful,
an HTTP-only cookie containing your [`StepUpToken`](./tokens#step-up-token) will be set automatically.
If [header authentication](./authentication#header-authentication) is enabled,
the [`StepUpToken`](./tokens#step-up-token) will be returned in the response body.
The [`TwoFactorAuthenticationToken`](./tokens#two-factor-authentication-token) will also be deleted.

### OAuth2

If [authentication via OAuth2 providers](./oauth2) is enabled and the user [connected a provider](./oauth2#connecting-an-oauth2-provider-to-an-existing-account),
users can request a step-up using their connected providers.
You can find more information [here](./oauth2#step-up-authentication).

## Status

You can request the current authentication status using [`GET /api/auth/status`](../../api/get-authentication-status.api.mdx).
This will check the cookies and headers you set in the request.


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

| Property                                      | Type      | Description                                                                                                                                           | Default value |
|-----------------------------------------------|-----------|-------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| singularity.auth.allow-header-authentication  | `Boolean` | Allow authentication using a bearer token placed in the header along HTTP-only Cookies. Allowed by default.                                           | `true`        |
| singularity.auth.prefer-header-authentication | `Boolean` | "If header authentication is allowed, this property controls the precedence: if true, the Authorization header is preferred over HTTP-only cookies.", | `true`        |


#### Example

```yaml
singularity:
  auth:
    allow-header-authentication: true
    prefer-header-authentication: true
```
