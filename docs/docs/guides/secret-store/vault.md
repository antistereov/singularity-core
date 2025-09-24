---
sidebar_position: 3
---

# HashiCorp Vault

[HashiCorp Vault](https://www.hashicorp.com/en/products/vault) is a centralized secrets management system that securely stores, 
manages, and controls access to sensitive data like API keys, passwords, and certificates. 
It is designed to reduce the risk of secrets being exposed in source code or insecure configuration files.

## Configuration

To configure the `Singularity` framework to use HashiCorp Vault,
you must set the `singularity.secrets.store` property to `VAULT` in your `application.yml` file. 
You then need to provide the connection details for your Vault instance.

```yaml
singularity:
  secrets:
    store: VAULT
    vault:
      scheme: "http"
      host: "localhost"
      port: 8200
      token: "s.your-vault-root-token"
      engine: "secret"
```

* `store`: Must be set to `VAULT` to enable the Vault secret store.
* `scheme`: The protocol to use for connecting to Vault (`http` or `https`).
* `host`: The hostname or IP address of the Vault server.
* `port`: The port on which the Vault server is listening. The default is `8200`.
* `token`: The authentication token used to access Vault. This token must have the necessary permissions to read and write secrets.
* `engine`: The name of the secrets engine to be used. By default, it is set to `secret`.

For local development and testing, you can quickly spin up a Vault instance in "dev" mode, which automatically handles initialization and provides a root token.