---
sidebar_position: 1
---

# Configuration

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

Singularity includes an easy-to-use email templating engine.
Some automated emails come out-of-the-box, such as an email verification email and a password reset email.

You can enable or disable emails.
By default, this feature is disabled.
Setting up or configuring an email server can be tricky.
Therefore, we decided to disable it by default to allow fast onboarding without much hustle.

You can choose to enable it any time. But make sure to configure it correctly.

| Property                 | Type      | Description                                                                                                    | Default |
|--------------------------|-----------|----------------------------------------------------------------------------------------------------------------|---------|
| singularity.email.enable | `Boolean` | Enable emails for email verification, password resets and much more. Make sure to configure your email server. | `false` |
