---
sidebar_position: 3
---

# Configuration


:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Singularity* is highly configurable. 
This allows you to use it as a baseline for any type of project.
It includes well-thought defaults that make your onboarding really smooth. 

This page covers application-specific configuration only.
If you need to configure, for example, the database,
make sure to check out the [corresponding page](../../docs/category/database) and the sidebar to see all features of this library.

## Application Configuration

These settings are required to make the app work as intended.

| Property                     | Type      | Description                                                                                                                                                | Default                 |
|------------------------------|-----------|------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| singularity.app.name         | `String`  | The name of the application. This is used in various places, including database names and email templates.                                                 | `Singularity`           |
| singularity.app.base-url     | `String`  | Base URL for the application. This is used to generate links to files, emails and other application resources. Do not use /api, use the base path instead. | `http://localhost:8000` |
| singularity.app.secure       | `Boolean` | Enable HTTPS and secure cookies for the application. Please set this to true in a production setup.                                                        | `false`                 |
| singularity.app.support-mail | `String`  | The email address users can contact for questions about your app.                                                                                          |                         |

### Root User

You can create a root user on startup if enabled and configured.

| Property                         | Type      | Description                                                     | Default |
|----------------------------------|-----------|-----------------------------------------------------------------|---------|
| singularity.app.create-root-user | `Boolean` | Should the application create a root user at application start? | `false` |
| singularity.app.root-email       | `String`  | The email associated to the root user.                          |         |
| singularity.app.root-password    | `String`  | The password associated to the root user.                       |         |

### Frontend

Most probably your application has a frontend. 
For both secure and easy integration, you can set the following parameters:

| Property                               | Type     | Description                                                                                                    | Default                 |
|----------------------------------------|----------|----------------------------------------------------------------------------------------------------------------|-------------------------|
| singularity.ui.base-url                | `String` | Base URL for the user interface of your application. Used as a trusted origin for CORS and in email templates. | `http://localhost:4200` |
| singularity.ui.icon-url                | `String` | URL to the application icon.                                                                                   |                         |
| singularity.ui.primary-color           | `String` | Primary color for the UI, used in email templates and other branding elements.                                 | `#6366f1`               |
| singularity.ui.contact-path            | `String` | Path to the contact page in your frontend application.                                                         | `/contact`              |
| singularity.ui.legal-notice-path       | `String` | Path to the legal notice page.                                                                                 | `/legal-notice`         |
| singularity.ui.privacy-policy-path     | `String` | Path to the privacy policy page.                                                                               | `/privacy-policy`       |
