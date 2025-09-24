---
sidebar_position: 1
---

# Connecting a Database

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Singularity* stores its data using [MongoDB](https://www.mongodb.com/).

MongoDB is a source-available, cross-platform, document-oriented database program. 
Classified as a NoSQL database program, MongoDB uses JSON-like documents with optional schemas. 
It is designed for developers who need to quickly build and deploy applications with a flexible and scalable database. 
MongoDB's key features include:

- **Document Model**: Stores data in flexible, JSON-like documents.
- **Scalability**: Supports horizontal scaling through sharding, distributing data across multiple servers.
- **High Availability**: Provides replica sets for automatic failover and data redundancy.
- **Rich Query Language**: Offers a powerful query language with a wide range of features.
- **Aggregation Framework**: Allows for complex data transformations and analysis.

## Quickstart

If you just want to test *Singularity*,
you can use the prebuilt and preconfigured [`docker-compose.yaml`](https://github.com/antistereov/singularity/blob/548bcba3ce6d0c1bdbacc2861c2726b1dc1d7991/libs/core/infrastructure/docker/docker-compose.yaml).
Just copy it into your project's root directory and run:

```shell
docker compose up -d
```

This will start a preconfigured instance of MongoDB.
It also includes a preconfigured instance of Redis which is the cache *Singularity* uses.
You can learn more about the database [here](../cache.md).

:::note
You can learn more about Docker and how to use it [here](https://docs.docker.com/).
:::

## Custom

For production and advanced use-cases you should provide your own instance of *MongoDB*.
You can learn more on how to install and set up the free *MongoDB Community Edition* [here](https://www.mongodb.com/docs/manual/administration/install-community).

Setting up an instance or even a replica set is out of scope for this guide.

After you installed and set up your database, you can use the **connection string** to
connect *Singularity* to *MongoDB*.
The string should look similar to this: `mongodb://username:password@localhost:27017/database`.

You can configure the connection in your `application.yaml` like this:

```yaml
spring:
  data:
    mongodb:
      # Enter your connection string here
      uri: mongodb://username:password@localhost:27017/database
```

:::note
*Singularity* uses [Spring Data MongoDB](https://spring.io/projects/spring-data-mongodb) to implement *MongoDB*.
:::
