# Axon Server migration tool

The Axon Server migration tool allows developers to migrate their existing Axon Framework event store, for example in
Postgres or MongoDB, to Axon Server Standard Edition or Enterprise Edition.

You can supply configuration to the tool by using command-lin parameters (such as `-Daxoniq.migration.source=RDBMS`), or
by adding an `application.properties` file in the working directory containing the wanted configuration.

The migration tool maintains the state of its migration, so it can be run multiple times. It also has built-in detection
for earlier broken batches. The tool can not be run in parallel to preserve the event store order.

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
or a Mongo instance. The source is dermined by the `axoniq.migration.source` and can be configured with either `RDBMS`
or `MONGO` currently. More information on specific properties for both can be found in the next sections.

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

Other options for the Spring Mongo library can be used as well.



## Destinations

We also need a place to store the events. This is always Axon Server. The destination differs in the manner which it is
stored; remote or locally.

The remote destination uses GRPC protocol to call Axon Server and store the events using the method also used by Axon
Framework internally.
This is sufficient for most use-cases.

The local destination stores the file directly on disk, without a running instance of Axon Server. After the migration
the event store can be used by the Axon Server instance. Because it lacks the need for remote communication and
replication logs, it is much
faster than the remote method.

### Remote

In order to use the remote method, define the following properties

| Property                       | Value                                                                    |
|--------------------------------|--------------------------------------------------------------------------|
| `axoniq.migration.destination` | `REMOTE`                                                                 |
| `axoniq.axonserver.servers`    | Comma separated list of hostnames and ports for the Axon Server cluster. |
| `axoniq.axonserver.context`    | The target context to migrate to, `default` by default.                  |
| `axoniq.axonserver.token`      | The access token, if access control is enabled.                          |

Any other Axon Framework properties for Axon Server can be used as well. This can be useful to configure certificates,
access control tokens or other settings.

### Local
The local method currently only supports migrating events.

| Property                               | Value                                                                                                   |
|----------------------------------------|---------------------------------------------------------------------------------------------------------|
| `axoniq.migration.destination`         | `LOCAL`                                                                                                 |
| `axoniq.axonserver.context`            | The context the events should be stored in                                                              |
| `axoniq.axonserver.event.storage`      | The storage directory where you want to store the events.                                               |
| `axoniq.axonserver.snapshot.storage`   | The storage directory where you want to store the events.                                               |
| `axoniq.axonserver.event.index-format` | Only for Axon Server EE: Set to `JUMP_SKIP` or `BLOOM`, depending on the index format you want to have. |

Do not migrate while Axon server is simultaneously running for that same context! This will lead to conflicts.

#### Axon Server Standard Edition

Follow these steps:
- Shut down Axon Server
- Delete the event store for a fresh start
- Set migration properties
- Run migration
- Start Axon Server

#### Axon Server Enterprise Edition

You should only migrate to Axon Server Enterprise edition with the Enterprise edition of this tool. This is included in the Enterprise edition download package and contains the improved JUMP_SKIP index mode. 

Follow these steps:
- Delete the context you want to migrate to, if it exists
- Set migration properties, including setting `axoniq.axonserver.event.index-format` to `JUMP_SKIP`
- Run migration
- Create the context and replication group (this will import the migrated store)

## Migrating tracking tokens

The migration tool only migrates the event store data to Axon Server. It does not update the tracking token values in
token_entry tables. Tracking tokens are highly dependent on the implementation of the actual event store used.
Migrating them is case specific and error-prone. Our recommendation is to reset the tracking processors after the
migration.
