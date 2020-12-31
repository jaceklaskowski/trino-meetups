# Trino on minikube

This document describes how to deploy [Trino](https://trino.io) to minikube.

Start `minikube`.

```text
minikube start
```

Point the shell (where you're going to build images) to minikube's Docker daemon.

```text
eval $(minikube -p minikube docker-env)
```

Build the Docker image of a trino coordinator and worker (using [docker build](https://docs.docker.com/engine/reference/commandline/build/) command).

**IMPORTANT** Don't use `latest` for the version part for reasons described later in the document.

```text
docker build \
    --tag warsaw-data-meetup/coordinator:beta0 \
    --build-arg nodetype=coordinator \
    --build-arg nodeenvname=minikube \
    .
```

```text
docker build \
    --tag warsaw-data-meetup/worker:beta0 \
    --build-arg nodetype=worker \
    --build-arg nodeenvname=minikube \
    .
```

```text
$ docker images "warsaw-data-meetup/*"
REPOSITORY                       TAG       IMAGE ID       CREATED          SIZE
warsaw-data-meetup/worker        beta0     c99878d7d340   6 seconds ago    1.06GB
warsaw-data-meetup/coordinator   beta0     f372359a77ce   33 seconds ago   1.06GB
```

Based on [Pushing images](https://minikube.sigs.k8s.io/docs/handbook/pushing/#1-pushing-directly-to-the-in-cluster-docker-daemon-docker-env) in the official documentation of minikube:

> Tip 1: Remember to turn off the `imagePullPolicy:Always` (use `imagePullPolicy:IfNotPresent` or `imagePullPolicy:Never`) in your yaml file. Otherwise Kubernetes won’t use your locally build image and it will pull from the network.

We'll skip that Tip 1 for now and try other options.

I sorted out the above replacing `latest` to `beta0` version (see [this answer of mine on StackOverflow](https://stackoverflow.com/a/65524919/1305344)).

```text
kubectl create deployment coordinator \
    --image=warsaw-data-meetup/coordinator:beta0 \
    --port=8080
```

```text
$ kubectl get deployments
NAME          READY   UP-TO-DATE   AVAILABLE   AGE
coordinator   1/1     1            1           8s
```

Let's access the UI.

`kubectl expose` using `LoadBalancer` type followed by [minikube tunnel](https://minikube.sigs.k8s.io/docs/handbook/accessing/#using-minikube-tunnel) in another terminal.

```text
kubectl expose deployment coordinator --type=LoadBalancer --port=8080
```

In a separate terminal `minikube tunnel` for service `coordinator`.

```text
minikube tunnel
```

Let the tunnel run.

```text
$ kubectl get svc
NAME          TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
coordinator   LoadBalancer   10.99.139.181   127.0.0.1     8080:30464/TCP   48s
kubernetes    ClusterIP      10.96.0.1       <none>        443/TCP          4m52s
```

The UI should be available at http://localhost:8080/ui/.

There should be no workers.

Deploy a worker.

```text
kubectl create deployment worker \
    --image=warsaw-data-meetup/worker:beta0
```

```text
$ kubectl get pods
NAME                           READY   STATUS    RESTARTS   AGE
coordinator-58dcbf59f9-tgl5f   1/1     Running   0          3m30s
worker-74ddf5ff9b-v2njw        1/1     Running   0          6s
```

```text
$ kubectl get rs
NAME                     DESIRED   CURRENT   READY   AGE
coordinator-58dcbf59f9   1         1         1       13m
worker-74ddf5ff9b        1         1         1       10m
```

Observe the logs using `kubectl logs` command.

```text
kubectl logs -f coordinator-58dcbf59f9-tgl5f
```

Should you want logging in to the pod, simply `kubectl exec`.

```text
kubectl exec -it coordinator-58dcbf59f9-tgl5f -- /bin/bash
```

In the end, clean up and delete the deployments.

```text
kubectl delete deployments/presto-coordinator
```

In order to clean up the environment fully `minikube delete` should make it.

```text
minikube delete
```
