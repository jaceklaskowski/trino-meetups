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

Note that this directory is mounted as a volume in Docker.

Start Presto coordinator only (until all issues are sorted out, and the comment is gone).

```text
nodeenvname=meetup docker-compose up \
  --build \
  --remove-orphans \
  coordinator
```
