# Axon Server migration tool

The Axon Server migration tool allows developers to migrate their existing Axon Framework event store, for example in
Postgres or MongoDB, to Axon Server.

There are two Spring profiles:

- `migrate-from-jpa`: Migrates from a RDBMS database, enabled by default
- `migrate-from-mongo`: Migrates from a Mongo Database

You can switch to the Mongo variant by running the application with the correct profile:

`java -jar axonserver-migration-tool.jar -Pmigrate-from-mongo`

The application will stay running until all snapshots and/or events are migrated. Parts of the migration can be disabled
using configuration. In addition, the tool can be configured to run indefinitely and keep polling for new events using a
configured timeout.

The migration tool maintains the state of its migration, so it can be run multiple times.

## Configuration

In order to find the correct source and desired output of the migration, some configuration is required. These can be
configured in an `application.properties` file in the working directory of the application.

| Property                                | Required | Description                                                                                                                                                                            |
|-----------------------------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `axoniq.axonserver.servers`             | Yes      | Comma separated list of hostnames and ports for the Axon Server cluster.                                                                                                               |
| `axoniq.datasource.eventstore.url`      | Yes      | Url of the JDBC data store containing the existing event store                                                                                                                         |
| `axoniq.datasource.eventstore.username` | Yes      | Username to connect to the JDBC data store containing the existing event store                                                                                                         |
| `axoniq.datasource.eventstore.password` | Yes      | Password to connect to the JDBC data store containing the existing event store                                                                                                         |
| `axon.serializer.events*=jackson`       | No       | The default settings expect the data in the current event store to be serialized using the XstreamSerializer. Add this property if the data is serialized using the JacksonSerializer. |
| `axoniq.migration.batchSize`            | No       | The batch size during the migration, defaults to 100                                                                                                                                   |
| `axoniq.migration.recentMillis`         | No       | The amount of milliseconds an event is considered recent. If the event is recent and a gap is detected, the migration will stop for safety.                                            |                                           |
| `axoniq.migration.migrateSnapshots`     | No       | Setting this property to false will disable the snapshot migration. Useful for only running the events migration.                                                                      |                                           |
| `axoniq.migration.migrateEvents`        | No       | Setting this property to true will disable the snapshot migration. Useful for only running the snapshot migration.                                                                     |                                           |
| `axoniq.migration.ignoredEvents`        | No       | A list of the payload types that should be ignored during the migration. NOTE: Be sure that this does not affect your aggregates in a bad way.                                         |                                           |
| `axoniq.migration.continuous`           | No       | Set this to true if you want the application to run indefinitely                                                                                                                       |                                           |
| `axoniq.migration.continuousTimeout`    | No       | Amount of time to sleep before retrying the migration using the continuous flag. Defaults to 100ms.                                                                                    |                                           |

Any other Axon Framework properties can be used as well. This can be useful to configure certificates, access control
tokens or other settings.

### Drivers

In addition, the application should be supplied a JDBC driver for the configured origin database.
This driver should be put in the `__dir__/libs` folder, with `__dir__` being the working directory of the application.

### Other configuration

Depending

## Various notes

Besides usage of the migration tool 

### Migrating tracking tokens

The migration tool only migrates the event store data to Axon Server. It does not update the tracking token values in
token_entry tables. Tracking tokens are highly dependent on the implementation of the actual event store used.
Migrating them is case specific and error-prone. Our recommendation is to reset the tracking processors after the
migration.
