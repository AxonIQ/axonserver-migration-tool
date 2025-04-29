# Axon Server migration tool

The Axon Server migration tool allows developers to migrate their existing Axon Framework event store, for example in
Postgres or MongoDB, to Axon Server Standard Edition or Enterprise Edition.

You can supply configuration to the tool by using command-lin parameters (such as `-Daxoniq.migration.source=RDBMS`), or
by adding an `application.properties` file in the working directory containing the wanted configuration.

The migration tool maintains the state of its migration, so it can be run multiple times. It also has built-in detection
for earlier broken batches. The tool can not be run in parallel to preserve the event store order.

> **Notice**: Before executing this migration tool, it is recommended to read the following blog post: 
> https://developer.axoniq.io/w/help-i-want-to-change-my-event-store



## Base configuration

By default, the application will migrate both events and snapshots. You can disable either by setting a property. In
addition, the batch size can be configured, as well as continuously running the tool.

| Property                                  | Default value | Note                                                                                                                                                                                          |
|-------------------------------------------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `axoniq.migration.migrateEvents`          | `true`        | Set to `false` to disable event migration                                                                                                                                                     |
| `axoniq.migration.migrateSnapshots`       | `true`        | Set to `false` to disable snapshot migration                                                                                                                                                  |
| `axoniq.migration.ignoredEvents`          | `''`          | Add a comma-separated list of (fully qualified) class names you want to skip during the migration. Useful if you have unwanted events in the store. Use with caution!                         |
| `axoniq.migration.continuous`             | `false`       | Set to `true` to run the tool in a loop continuously. Useful for migrations without downtime.                                                                                                 |
| `axoniq.migration.continuousTimeout`      | `100ms`       | Amount of time for the Thread to sleep until re-running the tool automatically.                                                                                                               |
| `axoniq.migration.recentMillis`           | `10000`       | Used to determine whether gaps are harmful for the consistency during the migration. If there is a gap within the last 10 seconds (by default), the tool will not process the batch and stop. |
| `axoniq.migration.reorderSequenceNumbers` | `false`       | Set to true if you have deleted events in your database in the past. This will leave gaps in aggregate sequence numbers and should be corrected during the migration.                         |
| `axon.serializer.events`                  | `XSTREAM`     | Which serializer to use for Metadata. XStream by default, can also be `JACKSON` or `DEFAULT`. Note that the events will be migrated as-is.                                                    |
|

A source and destination for the events and snapshots should also be configured, depending on your use-case.

Note: You can use environment variables in your properties, like so:
`my.property=${MY_ENV_VARIABLE}`

## Sources

You should define the source of the events and snapshots you want to migrate. Currently, this can be an RDBMS database
or a Mongo instance. The source is dermined by the `axoniq.migration.source` and can be configured with `RDBMS`, `MONGO`
or `AXONSERVER`.
More information on specific properties for both can be found in the next sections.

### RDBMS

In order to migrate from an RDBMS, the following properties should be supplied:

| Property                                  | Value                                                                                                                                                                            |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `axoniq.migration.source`                 | `RDBMS`                                                                                                                                                                          |
| `axoniq.datasource.eventstore.url`        | JDBC url of the database                                                                                                                                                         |
| `axoniq.datasource.eventstore.username`   | Username of the database                                                                                                                                                         |
| `axoniq.datasource.eventstore.password`   | Password of the database                                                                                                                                                         |
| `spring.jpa.properties.hibernate.dialect` | OPTIONAL. If you want to use a specific dialect, define this property. You can attach your own Jars containing a dialect in the `libs` folder. This will be automatically loaded |

The application should be supplied a JDBC driver for the configured origin database.
This driver should be put in the `libs` folder.


### Mongo

In order to migrate from a Mongo database, the following properties should be supplied:

| Property                       | Value                    |
|--------------------------------|--------------------------|
| `axoniq.migration.source`      | `MONGO`                  |
| `spring.data.mongodb.uri`      | URI of the database      |
| `spring.data.mongodb.username` | Username of the database |
| `spring.data.mongodb.password` | Password of the database |

### Axon Server

You can use Axon Server as a source, to migrate events from one Axon Server to another.
Note that snapshots won't be migrated, as Axon Server does not allow snapshots to be queried in this fashion.

In order to migrate from an Axon Server, the following properties should be supplied:

| Property                                     | Value                                                                    |
|----------------------------------------------|--------------------------------------------------------------------------|
| `axoniq.migration.source`                    | `AXONSERVER`                                                             |
| `axoniq.migration.source.axonserver.servers` | Comma separated list of hostnames and ports for the Axon Server cluster. |
| `axoniq.migration.source.axonserver.context` | The source context to migrate from, `default` by default.                |

Note that you can use any Axon Server property under `axoniq.migration.source.axonserver` to configure the connection to the Axon Server,
just like you would an Axon Framework application.

## Destinations

We also need a place to store the events. The remote destination uses GRPC protocol to call Axon Server and store the events using the method also used by Axon
Framework internally.

### Axon Server

In order to migrate events to Axon Server, define the following properties

| Property                                          | Value                                                                    |
|---------------------------------------------------|--------------------------------------------------------------------------|
| `axoniq.migration.destination`                    | `AXONSERVER`                                                             |
| `axoniq.migration.destination.axonserver.servers` | Comma separated list of hostnames and ports for the Axon Server cluster. |
| `axoniq.migration.destination.axonserver.context` | The target context to migrate to, `default` by default.                  |
| `axoniq.migration.destination.axonserver.token`   | The access token, if access control is enabled.                          |


Note that you can use any Axon Server property under `axoniq.migration.destination.axonserver` to configure the connection to the Axon Server,
just like you would an Axon Framework application.

## Migrating tracking tokens

The migration tool only migrates the event store data to Axon Server. It does not update the tracking token values in
token_entry tables. Tracking tokens are highly dependent on the implementation of the actual event store used.
Migrating them is case-specific and error-prone. Our recommendation is to reset the tracking processors after the
migration.
