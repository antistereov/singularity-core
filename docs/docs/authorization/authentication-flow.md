---
sidebar_position: 4
description: Learn how to register, log in or log out your users.
---

# Authentication Flow

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

## Registering Users

A new user can be registered by calling the endpoint [`POST /api/auth/register`](/swagger#/Authentication/register).
Users are uniquely identified by email address. 
An error response will be returned if a register request is send with an email that is already used.

After the registration succeeded, an HTTP-only cookie with **access token** and **refresh token** will be set automatically.
If [header authentication](/docs/authorization/basics#header-authentication) is enabled, access token and refresh token will be returned in the response body.

## Login

You can log in a user by calling [`POST /api/auth/login`](/swagger#/Authentication/login).

## Logout

## Step-Up
