---
sidebar_position: 2
---

# Local File Storage

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Singularity's* local file storage implementation allows you to store and manage files directly on the server's file system. It is designed to work seamlessly with the core `FileStorage` interface, providing a concrete, file-system-based backend.

## Configuration

To enable and configure local file storage, 
you must set the `singularity.file.storage.type` property to `LOCAL` in your `application.yml` file. 

### Storage Location

You can optionally change the **default storage location** by setting `singularity.file.storage.local.fileDirectory`.


### Example `application.yml`

```yaml
singularity:
  file:
    storage:
      type: local # Enables the LocalFileStorage implementation
      local:
        fileDirectory: "/srv/my-app/files" # Optional: customize the storage path
```
