---
sidebar_position: 1
---

# Authentication

Authentication is one of the core features of `singularity`. 
Mail verification and two-factor authentication are included out of the box.

Learn how you can use the predefined endpoints to authorize your users.

## Registering a User

The endpoint [`/api/user/register`](/swagger#/User%20Session/register) registers a user. 
If [Mail](/docs/configuration/mail) is configured and enabled, a verification email will be sent to the user.

After registration, an HTTP-only cookie will be set and 
if [header authentication](/docs/configuration/authentication#header-authentication) is enabled,
a token will be sent in the response.
