---
sidebar_position: 5
description: Learn more about articles.
---
# Articles

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

The **Article** is the primary content type in *Singularity* for managing editorial and blog-style content. 
It is engineered to fully utilize and extend the core Content Management System (CMS) architecture.

The Kotlin `data class` **`Article`** implements two foundational architectural interfaces:

1.  **[`ContentDocument<Article>`](introduction.md#core-content-object-structure):** Provides the base for security, access control, and tag management.
2.  **[`Translatable<ArticleTranslation>`](i18n.md):** Enables the article to store content in multiple languages.

## The Article Model Structure

The `Article` document stores both core CMS fields and specialized article metadata. The model's power comes from its structure, which separates editorial content (`translations`) from workflow and display metadata (`state`, `colors`).

| Field                                              | Type                              | Description                                                                                                                   |
|:---------------------------------------------------|:----------------------------------|:------------------------------------------------------------------------------------------------------------------------------|
| **`key`**, **`access`**, **`tags`**, **`trusted`** | (Inherited)                       | Core content features (Security, Ownership, Tagging) from [`ContentDocument`](introduction.md#core-content-object-structure). |
| **`translations`**                                 | `Map<Locale, ArticleTranslation>` | Stores localized fields (`title`, `content`, `summary`).                                                                      |
| **`publishedAt`**                                  | `Instant?`                        | The timestamp of when the article was formally published.                                                                     |
| **`path`**                                         | `String`                          | The unique, URL-friendly slug for the article (e.g., `/guides/my-new-article`).                                               |
| **`state`**                                        | `ArticleState`                    | The current workflow state, such as `DRAFT`, `PUBLISHED`, or `ARCHIVED`.                                                      |
| **`imageKey`**                                     | `String?`                         | The `FileMetadataDocument` key linking to the article's primary image/rendition set.                                          |
| **`colors`**                                       | `ArticleColors`                   | Optional metadata for frontend styling (text and background hex codes).                                                       |

### Specialized Article Sub-Models

#### `ArticleState`

This enum defines the formal state of the article, which is crucial for controlling public visibility and editorial workflows.

| State       | Description                                                                 | Usage Context                     |
|:------------|:----------------------------------------------------------------------------|:----------------------------------|
| `DRAFT`     | The article is currently being edited and is not publicly visible.          | Development, Review               |
| `PUBLISHED` | The article is live and accessible according to its `ContentAccessDetails`. | Production, Public Access         |
| `ARCHIVED`  | The article is no longer active but is retained for historical purposes.    | Historical records, Internal only |

#### `ArticleTranslation`

This structure holds the actual, localized editorial content fields that are expected to change per language.

| Field     | Type     | Description                                             |
|:----------|:---------|:--------------------------------------------------------|
| `title`   | `String` | The title of the article.                               |
| `summary` | `String` | A brief description or snippet used for overview pages. |
| `content` | `String` | The full body content of the article.                   |

#### `ArticleColors`

This model provides simple thematic styling control, which can be useful for frontend rendering, like custom branding on a news feed.

| Field        | Type      | Default     | Description                                             |
|:-------------|:----------|:------------|:--------------------------------------------------------|
| `text`       | `String?` | `"white"`   | The color used for the article's primary text.          |
| `background` | `String?` | `"#00008B"` | The background color (e.g., used for card backgrounds). |

## Key Advantages

### 1. Robust Multilingual Support (i18n)

By implementing the `Translatable` interface, Articles store all editorial text in a nested `translations` map, 
allowing for robust internationalization out-of-the-box.

:::note Translations
You can learn more about translations [here](i18n.md).
:::

:::info Deep Localization
When retrieving or searching Articles, the system automatically uses the requested `Locale` 
to focus on the `ArticleTranslation` fields. 
Search queries for `title` or `content` are **language-specific**, 
ensuring accurate results for the current user's language preference.
:::

### 2. Integrated Publishing Workflow

The `Article` model uses the `ArticleState` enum and the dedicated `changeState` endpoint to manage a formal editorial lifecycle.

:::note Controlled State Transitions
The endpoint `PUT /api/content/articles/{key}/state` is restricted to users with at least the **`EDITOR`** role. This separation ensures that only authorized personnel can move an article from `DRAFT` to `PUBLISHED`, providing essential governance over content release.
:::

### 3. Advanced, Localized Search & Filtering

The API for querying Articles (`GET /api/content/articles`) offers highly granular search capabilities through its comprehensive set of query parameters.

:::info Comprehensive Filtering
The `ArticleController` allows filtering by numerous criteria, including:
* Localized text (`title`, `content`)
* Date ranges (`createdAt`, `updatedAt`, `publishedAt`)
* Workflow State (`state`)
* **Roles:** The `roles` parameter allows users to filter the results to only include Articles for which they have a specific access level.
  :::

### 4. Inherited Security Model and Customization

As an implementation of `ContentDocument`, the Article automatically inherits the system's robust security framework.

:::note Access
You can learn more about access [here](introduction.md#detailed-access-structure).
:::

:::warning Secure Access by Default
An Article is never accessible unless granted permission via its `ContentAccessDetails`. This includes:
1.  Being set to **`PUBLIC`** visibility.
2.  Explicitly sharing with a **User** or **Group** with a role of **`VIEWER`** or higher (defined in `ContentAccessPermissions.kt`).
3.  The user possessing the global **`ADMIN`** role.

The `trusted` flag, also inherited from `ContentDocument`, provides an extra layer of integrity control for critical assets.
:::

## Configuration

Articles are enabled by default. You can disable it by changing the following property:

| Property                            | Type      | Description                | Default value |
|-------------------------------------|-----------|----------------------------|---------------|
| singularity.content.articles.enable | `Boolean` | Enable article management. | `true`        |


## Management

### Creating Articles

New articles can be created through the endpoint:

* **Endpoint:** [`POST /api/content/articles`](../../api/create-article.api.mdx)
* **Requirements:** Only members of the [`CONTRIBUTOR`](introduction.md#global-server-group-contributor) can create articles.


### Finding Articles

:::info
These endpoints return only the articles that are accessible by the requester.
You can learn more [here](../content/introduction.md#authorization-logic).
:::

* You can request an article by its `key` through the endpoint
  [`GET /api/content/articles/{key}`](../../api/get-article-by-key.api.mdx).
* You can request and filter articles through the endpoint
  [`GET /api/content/articles`](../../api/get-articles.api.mdx).
* The extended access details for an article with `key` are accessible through the endpoint
  [`GET /api/content/articles/access`](../../api/get-content-object-access-details.api.mdx).


### Updating Articles

#### Title, Content, Summary, Colors

You can update the **title**, **content**, **summary**, **tags** and **colors** of an article with given `key`
through the endpoint:

* **Endpoint:** [`PATCH /api/content/articles/{key}`](../../api/update-article.api.mdx)
* **Requirements:** Only [`EDITOR`](../content/introduction.md#object-specific-roles-shared-state)s of the requested article
  can perform this action.

:::note Locale
The `locale` specifies which translation should be updated.
The `locale` query parameter specifies which translation should be returned.
:::

#### State

You can update the article's [state](#articlestate) through the endpoint:

* **Endpoint:** [`PUT /api/content/articles/{key}/state`](../../api/update-article-state.api.mdx)
* **Requirements:** Only [`EDITOR`](../content/introduction.md#object-specific-roles-shared-state)s of the requested article
  can perform this action.

#### Image

You can update the article's image through the endpoint:

* **Endpoint:** [`PUT /api/content/articles/{key}/image`](../../api/update-article-image.api.mdx)
* **Requirements:** Only [`EDITOR`](../content/introduction.md#object-specific-roles-shared-state)s of the requested article
  can perform this action.

#### Owner

The **owner** of an article with given `key` can be updated through the endpoint:

* **Endpoint:** [`PUT /api/content/articles/{key}/owner`](../../api/update-content-object-owner.api.mdx)
* **Requirements:** Only the current owner is permitted to perform this action.

#### Access

The **access** can be updated through the endpoint:

* **Endpoint:** [`PUT /api/content/articles/{key}/access`](../../api/update-content-object-access.api.mdx)
* **Requirements:** Only [`MAINTAINER`](../content/introduction.md#object-specific-roles-shared-state)s of the requested article
  can perform this action.

#### Trusted State

The **trusted state** can be updated through the endpoint
* **Endpoint:**  [`PUT /api/content/articles/{key}/trusted`](../../api/update-content-object-trusted-state.api.mdx)
* **Requirements:** Only [`ADMIN`](../auth/roles.md#admins)s can perform this action.

### Deleting Articles

An article with given `key` can be deleted through the endpoint:

* **Endpoint:** [`DELETE /api/content/articles/{key}`](../../api/delete-content-object-by-key.api.mdx)
* **Requirements:** Only [`MAINTAINER`](../content/introduction.md#object-specific-roles-shared-state)s of the requested article
  can perform this action.

