# Presto Meetup Connector

## Building

Execute the following:

```text
sbt clean assembly
```

You should have the connector under `target/scala-2.13`:

```text
presto-meetup-connector-assembly-0.1.jar
```

This directory is mounted as a volume in Docker.

Start Presto coordinator and worker (no need for other services).

```text
nodeenvname=meetup docker-compose up \
  --build \
  --remove-orphans \
  coordinator worker
```

List the available catalogs. Among them should be `meetup`.

```text
$ docker-compose exec coordinator presto \
    --execute="SHOW CATALOGS LIKE 'meetup'"
"meetup"

$ docker-compose exec coordinator presto --execute="SHOW SCHEMAS IN meetup"
"information_schema"
```

Observe the logs.

```text
>>> [meetup.MeetupConnector] MeetupConnector.beginTransaction(READ UNCOMMITTED, readOnly=false)
>>> [meetup.MeetupConnector] ...handle=meetup.MeetupConnector$$anon$1@2d98e0f4
>>> [meetup.MeetupConnector] MeetupConnector.getMetadata(meetup.MeetupConnector$$anon$1@2d98e0f4)
```
