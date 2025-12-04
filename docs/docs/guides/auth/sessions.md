---
sidebar_position: 8
description: Learn how a user can manage their sessions.
---

# Sessions

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

Every new device or browser a user logs into creates a new **session**. 
Each session is uniquely identified by a [`RefreshToken`](./tokens#refresh-token).

The [`RefreshToken`](./tokens#refresh-token) is a long-lived credential stored securely on the user's device. 
Its primary purpose is to obtain new [`AccessToken`](./tokens#access-token) without requiring the user to re-enter their credentials every time.

An [`AccesstToken`](./tokens#access-token) is a short-lived token used to authenticate API requests. 
It has a short expiration time (e.g., minutes or hours), 
which significantly reduces the security risk if it's intercepted. 
When an [`AccesstToken`](./tokens#access-token) expires, 
the application uses the [`RefreshToken`](./tokens#refresh-token) to automatically get a new one from your authentication server. 
This process is seamless to the user.

## Active Sessions

You can retrieve the user's current active sessions through
[`GET /api/auth/session`](../../api/get-active-sessions.api.mdx) using a valid [`AccesstToken`](./tokens#access-token).

Each session has a unique ID.

## Invalidating Sessions

Users can **invalidate** sessions for security reasons, like when a device is lost or stolen. 
When a user invalidates a session, *Singularity* performs two key actions:

1. The associated [`RefreshToken`](./tokens#refresh-token) is immediately marked as invalid. 
    It can no longer be used to request new [`AccesstToken`](./tokens#access-token).
2. All currently active [`AccesstToken`](./tokens#access-token) linked to that specific session are instantly revoked. 
    This ensures that any ongoing API requests using those tokens will fail, immediately cutting off access from the compromised device.

This approach provides a robust security layer, 
allowing users to remotely log out of specific devices without having to change their password.

### Invalidating a Specific Session

You can invalidate a specific session using the session's ID through the endpoint
[`DELETE /api/auth/sessions/<session-id>`](../../api/delete-session.api.mdx)
with a valid [`AccesstToken`](./tokens#access-token).

### Invalidating all Session

You can invalidate all sessions through [`DELETE /api/auth/sessions`](../../api/delete-all-sessions.api.mdx)
with a valid [`AccesstToken`](./tokens#access-token).
