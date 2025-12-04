---
sidebar_position: 7
description: Learn how to configure automated security email alerts for user accounts.
---

# Security Alerts

:::warning
All security alerts require email to be enabled and configured in your application.
:::

To keep users informed of critical account activity and help them quickly identify potential compromises, *Singularity* supports automated email security alerts.

These alerts are sent for high-impact events, ensuring users are immediately aware of changes to their authentication methods and access points.

:::info
These emails include references to pages of the frontend application
such as a link to the account security settings or the login page.

These paths can be configured [here](../configuration.md#paths).
:::

## Core Identity Alerts

These alerts cover fundamental changes to the user's primary login credentials and security settings.

* **New Login (`login`)**: Sent when a successful login occurs from a previously unrecognized device or location.
  This alert will be sent after successfully requesting the endpoint [`POST /api/auth/login`](../../api/login.api.mdx).
* **Registration with existing email (`registration-with-existing-email`):** Sent when trying to register
  a new account using an email address that is already associated with an existing account through the endpoint
  [`POST /api/auth/register`](../../api/register.api.mdx).
* **Password Change (`password-changed`)**: Sent when the user successfully changes their password.
  This alert will be sent after successfully requesting the endpoint [`POST /api/users/me/password`](../../api/change-password-of-authorized-user.api.mdx).
* **Email Change (`email-changed`)**: Sent when the user successfully changes their email address.
  This alert will be sent after successfully requesting the endpoint [`POST /api/auth/email/verification`](../../api/verify-email.api.mdx).

:::note
These emails include links to the account security settings and login page of the frontend application.
These links can be configured [here](../configuration.md#paths).
:::

## 2FA Specific Alerts

:::note
You can learn more about 2FA [here](./two-factor.md).
:::

* **2FA Added (`two-factor-added`)**: Sent upon the successful addition of a new Two-Factor Authentication method (TOTP or Email). 
  Adding a new 2FA method is a critical setting, and all active sessions are terminated upon completion.
  This alert will be sent after successfully requesting the endpoint [`POST /api/auth/2fa/totp/setup`](../../api/enable-totp-as-two-factor-method.api.mdx)
  for [TOTP](./two-factor.md#totp) or [`POST /api/auth/2fa/email/enable`](../../api/enable-email-as-two-factor-method.api.mdx) for [email](./two-factor.md#email).
* **2FA Removed (`two-factor-removed`)**: Sent when an existing 2FA method is removed.
  This alert will be sent after successfully requesting the endpoint [`DELETE /api/auth/2fa/totp`](../../api/disable-totp-as-two-factor-method.api.mdx) or [`DELETE /api/auth/2fa/email`](../../api/disable-email-as-two-factor-method.api.mdx).

:::note
These emails include a link to the account security settings of the frontend application.
These links can be configured [here](../configuration.md#paths).
:::

## OAuth2 Specific Alerts

:::note
You can learn more about OAUth2 [here](./oauth2.md).
:::

The alerts for connecting and disconnecting OAuth2 providers relate directly to the user's ability to log in using external services.

* **Provider Connection (`oauth2-provider-connected`)**: Sent when a user successfully links a new external provider (like GitHub or Google) to their account, which happens during registration or when managing providers.
* **Provider Disconnection (`oauth2-provider-disconnected`)**: Sent when a user removes a linked external provider using the [`DELETE /api/users/me/providers/<provider-name>`](../../api/delete-identity-provider.api.mdx) endpoint.
  
:::warning
A user is generally not allowed to disconnect an OAuth2 identity if it is their only means of authentication.
:::

:::note
These emails include a link to the account security settings of the frontend application.
These links can be configured [here](../configuration.md#paths).
:::

## User Information

To improve the user experience, *Singularity* will send the following emails:

### No Account Information

If a user tries to perform one of the following security actions:

* requesting a password reset [`POST /api/auth/password/reset-request`](../../api/send-password-reset-email.api.mdx) or
* verifying an email address via [`POST /api/auth/email/verification`](../../api/verify-email.api.mdx)

an email will be sent to the user notifying that there is no account associated with the provided email address.

:::note
This email includes a link to the registration page of the frontend application.
These links can be configured [here](../configuration.md#paths).
:::

### Identity Provider Information

If a user tries to log in through a method the user did not set up, an email will be sent 
that informs the user which methods are configured.

This will limit confusion if a user thinks they already signed up but doesn't seem to remember the correct password or which provider they connected with.

It will be sent in the following scenarios:

* login with password through [`POST /api/auth/login`](../../api/login.api.mdx) or
* [registration via an OAuth2 provider](./oauth2.md#registration) and
  * the provider account is not yet registered in the application
  * the email address associated with the provider account is already associated with an existing account

:::note
This email includes a link to the login page of the frontend application.
These links can be configured [here](../configuration.md#paths).
:::

## Configuration

You can enable or disable automated email alerts for various security-critical events using the following properties:

| Property                                                         | Type      | Description                                                                                                                                               | Default value |
|------------------------------------------------------------------|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|---------------|
| singularity.auth.security-alert.login                            | `Boolean` | Enable automated email alerts for new logins. Alerts are typically sent when a successful login occurs from a previously unrecognized device or location. | `true`        |
| singularity.auth.security-alert.registration-with-existing-email | `Boolean` | Enable automated email alerts for tries to register a new account with an email address that is already linked to an existing account.                    | `true`        |
| singularity.auth.security-alert.email-changed                    | `Boolean` | Enable automated email alerts for an email change.                                                                                                        | `true`        |
| singularity.auth.security-alert.password-changed                 | `Boolean` | Enable automated email alerts for a password change.                                                                                                      | `true`        |
| singularity.auth.security-alert.two-factor-added                 | `Boolean` | Enable automated email alerts for the addition of a new 2FA method.                                                                                       | `true`        |
| singularity.auth.security-alert.two-factor-removed               | `Boolean` | Enable automated email alerts for the removal of an existing 2FA method.                                                                                  | `true`        |
| singularity.auth.security-alert.oauth2-provider-connected        | `Boolean` | Enable automated email alerts for the connection of a new OAuth2 provider.                                                                                | `true`        |
| singularity.auth.security-alert.oauth2-provider-disconnected     | `Boolean` | Enable automated email alerts for the disconnection of an existing OAuth2 provider.                                                                       | `true`        |
