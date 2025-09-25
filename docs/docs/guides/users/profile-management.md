---
sidebar_position: 2
description: Learn how to update user information.
---

# Profile Management

The Profile Management system provides a set of tools that allow an authenticated user to view and update their own account information.

All operations in this section are self-service, meaning they are performed by a user on their own account. They require a valid `AccessToken` for authentication.

## Core Functions

The system supports the following key operations for managing a personal profile:

### Profile Retrieval

This is accessible to all authenticated users and can be requested through [`GET /api/auth/users/me`](../../api/get-authorized-user.api.mdx).

You can retrieve the full profile information of the currently authenticated user. 
This includes personal details and connected identity providers.

### Profile Update

This action is accessible to all authenticated users and can be requested through [`PUT /api/auth/users/me`](../../api/update-authorized-user.api.mdx).

You can update various fields of your profile, such as your username or other non-sensitive information.

### Email Management

The system provides dedicated functionality for changing your email address. 
This is a sensitive operation and requires a [`StepUpToken`](../auth/tokens.md#step-up-token) in addition to your [`AccessToken`](../auth/tokens.md#access-token).

#### Changing Email

Your new email address will undergo a verification process. 
If email is enabled, a verification token will be sent to the new address. 
The change will only be finalized once the token is verified. If email is disabled, the change is instant.

This can be requested through [`PUT /api/auth/users/me/email`](../../api/change-email-of-authorized-user.api.mdx).

### Password Management

For security, changing your password is a dedicated process that requires 
both an [`AccessToken`](../auth/tokens.md#access-token) and a [`StepUpToken`](../auth/tokens.md#step-up-token).

#### Changing Password

You can set a new password for your account, which must meet the specified password policy 
(minimum length, character types, etc.).
This can be requested through [`PUT /api/auth/users/me/password`](../../api/change-password-of-authorized-user.api.mdx).

### Avatar Management

You can manage your user avatar through two separate actions.

#### Updating the Avatar

You can upload a new image to be used as your profile picture.
This can be requested through [`PUT /api/auth/users/me/avatar`](../../api/set-avatar-of-authorized-user.api.mdx).

#### Deleting the Avatar

You can remove your current avatar.
This can be requested through [`DELETE /api/auth/users/me/avatar`](../../api/delete-avatar-of-authorized-user.api.mdx).

### Account Deletion

For security, deleting your own account is a sensitive action 
that requires a [`StepUpToken`](../auth/tokens.md#step-up-token) in addition to your [`AccessToken`](../auth/tokens.md#access-token). 
This is a permanent and irreversible action. Upon successful deletion, 
your session will be terminated and all associated authentication cookies will be cleared from your browser.

This can be requested through [`DELETE /api/auth/users/me`](../../api/delete-authorized-user.api.mdx).