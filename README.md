# Presto Dockerized

Replace `nodeenvname` with the name of the environment. It is used to build the Presto images.

```text
nodeenvname=readme docker-compose up --build --remove-orphans
```

```text
docker-compose ps
```

Open http://localhost:8080/ui/. You should see one Presto worker.

```text
docker-compose up --scale worker=3 worker
```

Review the number of workers in http://localhost:8080/ui/. You should see three Presto workers now.

```text
docker-compose ps
```

```text
docker-compose exec coordinator presto
```
