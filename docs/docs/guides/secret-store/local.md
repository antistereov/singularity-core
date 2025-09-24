---
sidebar_position: 2
---

# Local Secret Store

The `Singularity` framework's `SecretStore` is designed to be pluggable, with a default implementation that uses a 
local [H2 database](https://h2database.com/html/main.html) for persistence. 
This implementation is ideal for local development and testing environments.

## How it Works

The local secret store uses an embedded H2 database to persist secrets. 
It handles the creation of the database and the necessary table on application startup. 
Secrets are stored in a file-based database within a configurable directory on the local file system.

This implementation is automatically enabled if the `singularity.secrets.store` property is set to `LOCAL` or is not specified at all, as it is the default value.

## Configuration

You can customize the directory where the secrets database is stored via the `application.yml` file.

```yaml
singularity:
  secrets:
    store: LOCAL
    local:
      secretDirectory: "./.data/secrets"
```