---
sidebar_position: 7
description: Learn how to use groups to control access.
---

# Groups

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Groups* allow fine-grained control on which information each user is allowed to see.

## Configuration

You can configure groups to be available after the first application startup.

| Property               | Type           | Description                                         | Default |
|------------------------|----------------|-----------------------------------------------------|---------|
| singularity.app.groups | `List<String>` | Groups that will be created on application startup. |         |
