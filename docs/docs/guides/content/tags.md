---
sidebar_position: 3
description: Learn more about tags.
---

# Tags

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

**Tags** allow users to categorize content in a quick and easy way.
Each content object contains the `tag` property with a list of tag keys 
that the object is categorized by.

A **Tag** is uniquely identified by a `key`.
You can save a name and a description in multiple [translations](i18n.md) to the database.

## Managing Tags

### Creating Tags

New tags can be created through the endpoint:

* **Endpoint:** [`POST /api/content/tags`](../../api/create-tag.api.mdx)
* **Requirements:** Only members of the [`CONTRIBUTOR`](introduction.md#global-server-group-contributor) can create tags.

#### On Startup

It is possible to create tags on startup.
You can use your `application.yaml` to specify tags with descriptions.

```yaml
singularity:
  content:
    tags:
      - key: music # The unique key
        translations:
          en: # The language tag
            name: Music
            description: Content related to music.
          de:
            name: Musik
            description: Inhalte kategorisiert in Musik.
      - key: arts
        translations:
          en:
            name: Arts
            description: Content related to arts.
          de:
            name: Kunst
            description: Inhalte kategorisiert in Kunst.
```

### Finding Tags

* A tag with given `key` can be requested through the endpoint [`GET /api/content/tags/{key}`](../../api/get-tag-by-key.api.mdx).
* Tags can be queried through the endpoint [`GET /api/content/tags`](../../api/get-tags.api.mdx).

### Updating Tags

A tag with given `key` can be updated through the endpoint:

* **Endpoint:** [`PATCH /api/content/tags/{key}`](../../api/update-tag.api.mdx)
* **Requirements:** Only members of the [`CONTRIBUTOR`](introduction.md#global-server-group-contributor) can update tags.

### Deleting Tags

A tag with given `key` can be deleted through the endpoint:

* **Endpoint:** [`DELETE /api/content/tags/{key}`](../../api/delete-tag.api.mdx)
* **Requirements:** Only members of the [`CONTRIBUTOR`](introduction.md#global-server-group-contributor) can update tags.
