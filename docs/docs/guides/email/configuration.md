---
sidebar_position: 1
---

# Configuration

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

_Singularity_ includes an easy-to-use email [templating engine](./templates.md).
Some automated emails come out-of-the-box, 
such as an email verification email and a password reset email.

:::info 
By default, this feature is disabled.
Setting up or configuring an email server can be tricky.
Therefore, we decided to disable it by default to allow fast onboarding without much hustle.

You can choose to enable it any time. But make sure to configure it correctly.
:::

:::warning
If email is disabled, some features such as password reset or email verification will **not work at all**.
:::

### Properties

| Property                              | Type      | Description                                            | Default Value    |
|:--------------------------------------|:----------|:-------------------------------------------------------|:-----------------|
| `singularity.email.enable`            | `Boolean` | Enable email functionality.                            | `false`          |
| `singularity.email.host`              | `String`  | The hostname of the email server.                      | `host.com`       |
| `singularity.email.port`              | `Int`     | The port for the email server.                         | `0`              |
| `singularity.email.email`             | `String`  | The sender's email address.                            | `email@host.com` |
| `singularity.email.username`          | `String`  | The username for authentication.                       | `email@host.com` |
| `singularity.email.password`          | `String`  | The password for authentication.                       | `password`       |
| `singularity.email.transportProtocol` | `String`  | The transport protocol, e.g., `smtp`.                  | `smtp`           |
| `singularity.email.smtpAuth`          | `Boolean` | Whether SMTP authentication is enabled.                | `true`           |
| `singularity.email.smtpStarttls`      | `Boolean` | Whether to use `STARTTLS` for a secure connection.     | `true`           |
| `singularity.email.debug`             | `Boolean` | Whether to enable debug output for the mail client.    | `false`          |
| `singularity.email.sendCooldown`      | `Long`    | The cooldown period in seconds between sending emails. | `60`             |

### Example `application.yaml`

```yaml
singularity:
  email:
    enable: true
    host: smtp.your-provider.com
    port: 587
    email: your-email@your-domain.com
    username: your-email@your-domain.com
    password: your-password
    transportProtocol: smtp
    smtpAuth: true
    smtpStarttls: true
    debug: false
    sendCooldown: 60
```
