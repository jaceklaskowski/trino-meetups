# Trino on minikube

This document describes how to deploy [Trino](https://trino.io) to minikube.

## Starting minikube

Start `minikube`.

```text
minikube start --cpus=4 --memory=8g
```

**TIP** You may want to use `minikube config options` to set these CPU cores and memory requirements permanently.

## Building Trino Images

Switch to another terminal and point the shell to minikube's Docker daemon. This step is to allow pushing images to minikube's not local Docker daemon.

```text
eval $(minikube -p minikube docker-env)
```

Review the available Docker images. The list depends on the version of minikube.

```text
$ docker images
REPOSITORY                                TAG        IMAGE ID       CREATED         SIZE
kubernetesui/dashboard                    v2.1.0     9a07b5b4bfac   3 weeks ago     226MB
k8s.gcr.io/kube-proxy                     v1.20.0    10cc881966cf   3 weeks ago     118MB
k8s.gcr.io/kube-scheduler                 v1.20.0    3138b6e3d471   3 weeks ago     46.4MB
k8s.gcr.io/kube-controller-manager        v1.20.0    b9fa1895dcaa   3 weeks ago     116MB
k8s.gcr.io/kube-apiserver                 v1.20.0    ca9843d3b545   3 weeks ago     122MB
gcr.io/k8s-minikube/storage-provisioner   v4         85069258b98a   4 weeks ago     29.7MB
k8s.gcr.io/etcd                           3.4.13-0   0369cf4303ff   4 months ago    253MB
k8s.gcr.io/coredns                        1.7.0      bfe3a36ebd25   6 months ago    45.2MB
kubernetesui/metrics-scraper              v1.0.4     86262685d9ab   9 months ago    36.9MB
k8s.gcr.io/pause                          3.2        80d28bedfe5d   10 months ago   683kB
```

Build the Docker images of a trino coordinator and worker (using [docker build](https://docs.docker.com/engine/reference/commandline/build/) command).

**TIP:** Review the `config.properties` files of coordinator and worker and learn about environment variables support in Trino as well as how Kubernetes makes communication between pods possible and reliable.

**IMPORTANT** Don't use `latest` for the version part in `--tag` option for reasons described later in the document.

```text
docker build \
    --tag warsaw-data-meetup/coordinator:350 \
    --build-arg prestoVersion=350 \
    --build-arg nodetype=coordinator \
    --build-arg nodeenvname=minikube \
    .
```

```text
docker build \
    --tag warsaw-data-meetup/worker:350 \
    --build-arg prestoVersion=350 \
    --build-arg nodetype=worker \
    --build-arg nodeenvname=minikube \
    .
```

Review the available Docker images. Make sure that the trino images are there.

```text
$ docker images "warsaw-data-meetup/*"
REPOSITORY                       TAG       IMAGE ID       CREATED          SIZE
warsaw-data-meetup/worker        350       6bfcd54acb3f   18 seconds ago   1.06GB
warsaw-data-meetup/coordinator   350       e18b4c49bbc3   31 seconds ago   1.06GB
```

Based on [Pushing images](https://minikube.sigs.k8s.io/docs/handbook/pushing/#1-pushing-directly-to-the-in-cluster-docker-daemon-docker-env) in the official documentation of `minikube`:

> Tip 1: Remember to turn off the `imagePullPolicy:Always` (use `imagePullPolicy:IfNotPresent` or `imagePullPolicy:Never`) in your yaml file. Otherwise Kubernetes won’t use your locally build image and it will pull from the network.

We'll keep that tip in mind, but skip it for now and simply rely on replacing `latest` to a non-`latest` version (see [this answer of mine on StackOverflow](https://stackoverflow.com/a/65524919/1305344)).

## Deploying Trino Coordinator

Create a Kubernetes [Deployment](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/) for the trino coordinator.

Declaratively:

```text
kubectl apply -f minikube/coordinator-deployment.yml
```

Imperatively (left mostly for demo purposes):

```text
kubectl create deployment trino-coordinator \
    --image=warsaw-data-meetup/coordinator:350 \
    --port=8080
```

Check if the Deployment was created.

```text
$ kubectl get deploy
NAME                READY   UP-TO-DATE   AVAILABLE   AGE
trino-coordinator   1/1     1            1           15s
```

```text
$ kubectl rollout status deployment/trino-coordinator
deployment "trino-coordinator" successfully rolled out
```

```text
$ kubectl get pods --show-labels
NAME                                READY   STATUS    RESTARTS   AGE   LABELS
trino-coordinator-9b648ddbf-b8s5q   1/1     Running   0          34s   app=coordinator,pod-template-hash=9b648ddbf
```

Review the ReplicaSet (`rs`) created by the Deployment.

```text
$ kubectl get rs
NAME                          DESIRED   CURRENT   READY   AGE
trino-coordinator-9b648ddbf   1         1         1       49s
```

Show the details of the Deployment using `kubectl describe` command.

```text
$ kubectl describe deployments/trino-coordinator
Name:                   trino-coordinator
Namespace:              default
CreationTimestamp:      Fri, 01 Jan 2021 15:56:28 +0100
Labels:                 app=coordinator
Annotations:            deployment.kubernetes.io/revision: 1
Selector:               app=coordinator
Replicas:               1 desired | 1 updated | 1 total | 1 available | 0 unavailable
StrategyType:           RollingUpdate
MinReadySeconds:        0
RollingUpdateStrategy:  25% max unavailable, 25% max surge
Pod Template:
  Labels:  app=coordinator
  Containers:
   coordinator:
    Image:        warsaw-data-meetup/coordinator:350
    Port:         8080/TCP
    Host Port:    0/TCP
    Environment:  <none>
    Mounts:       <none>
  Volumes:        <none>
Conditions:
  Type           Status  Reason
  ----           ------  ------
  Available      True    MinimumReplicasAvailable
  Progressing    True    NewReplicaSetAvailable
OldReplicaSets:  <none>
NewReplicaSet:   trino-coordinator-9b648ddbf (1/1 replicas created)
Events:
  Type    Reason             Age   From                   Message
  ----    ------             ----  ----                   -------
  Normal  ScalingReplicaSet  61s   deployment-controller  Scaled up replica set trino-coordinator-9b648ddbf to 1
```

Observe the logs using `kubectl logs` command (possibly with `-f` option).

```text
kubectl logs trino-coordinator-9b648ddbf-b8s5q
```

You should see logs similar to the following:

```text
2021-01-01T14:56:42.012Z	INFO	main	io.prestosql.security.AccessControlManager	Using system access control default
2021-01-01T14:56:42.044Z	INFO	main	io.prestosql.server.Server	======== SERVER STARTED ========
```

Congratulations! You've just deployed a Trino coordinator to minikube!

## Accessing trino UI

Let's access the UI of the trino coordinator.

`kubectl expose` using `LoadBalancer` type (as described in [Creating a Service](https://kubernetes.io/docs/concepts/services-networking/connect-applications-service/#creating-a-service)) followed by [minikube tunnel](https://minikube.sigs.k8s.io/docs/handbook/accessing/#using-minikube-tunnel) in another terminal.

```text
kubectl expose deployment/trino-coordinator --type=LoadBalancer
```

Review the service and notice `EXTERNAL-IP` column with `<pending>` entry. That's perfectly fine.

```text
$ kubectl get svc trino-coordinator
NAME                TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
trino-coordinator   LoadBalancer   10.108.104.73   <pending>     8080:30797/TCP   6s
```

Let's assign an `EXTERNAL-IP`. In a separate terminal run `minikube tunnel` for `trino-coordinator` service.

```text
$ minikube tunnel
🏃  Starting tunnel for service trino-coordinator.
```

Let the tunnel run.

Check out the `EXTERNAL-IP`.

```text
$ kubectl get svc trino-coordinator
NAME                TYPE           CLUSTER-IP      EXTERNAL-IP   PORT(S)          AGE
trino-coordinator   LoadBalancer   10.108.104.73   127.0.0.1     8080:30797/TCP   2m55s
```

The UI should be available at http://localhost:8080/ui/.

There should be no workers. No worries. You take care of this in a bit.

## Exposing Coordinator Cluster-Wide

```text
kubectl expose deployment/trino-coordinator --name=trino-clusterip
```

Check the status using `kubectl get svc` command. Note `TYPE`, `CLUSTER-IP` and `EXTERNAL-IP` columns.

```text
$ kubectl get svc trino-clusterip
NAME              TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)    AGE
trino-clusterip   ClusterIP   10.111.118.252   <none>        8080/TCP   11s
```

That makes the trino coordinator available on that `CLUSTER-IP` address cluster-wide (regardless how many times the underlying pod gets restarted and assigned a different IP then).

You can access the networking setup of the service using [Environment Variables](https://kubernetes.io/docs/concepts/services-networking/connect-applications-service/#environment-variables). Restart pods that were started before a service was created.

**NOTE:** The following steps are optional as the coordinator does not use this and the earlier services.

```text
kubectl scale deployment trino-coordinator --replicas 0
kubectl scale deployment trino-coordinator --replicas 1
```

```text
$ kubectl exec trino-coordinator-9b648ddbf-rrdsd -- printenv | grep TRINO_
TRINO_COORDINATOR_SERVICE_HOST=10.102.77.174
TRINO_COORDINATOR_PORT_8080_TCP=tcp://10.102.77.174:8080
TRINO_CLUSTERIP_PORT_8080_TCP_PROTO=tcp
TRINO_CLUSTERIP_PORT_8080_TCP_ADDR=10.106.104.179
TRINO_COORDINATOR_PORT_8080_TCP_PROTO=tcp
TRINO_CLUSTERIP_PORT_8080_TCP_PORT=8080
TRINO_COORDINATOR_PORT=tcp://10.102.77.174:8080
TRINO_COORDINATOR_PORT_8080_TCP_PORT=8080
TRINO_COORDINATOR_PORT_8080_TCP_ADDR=10.102.77.174
TRINO_CLUSTERIP_SERVICE_HOST=10.106.104.179
TRINO_CLUSTERIP_PORT=tcp://10.106.104.179:8080
TRINO_COORDINATOR_SERVICE_PORT=8080
TRINO_CLUSTERIP_SERVICE_PORT=8080
TRINO_CLUSTERIP_PORT_8080_TCP=tcp://10.106.104.179:8080
```

## Deploying Trino Worker

Deploy a Trino worker.

```text
kubectl apply -f minikube/worker-deployment.yml
```

```text
$ kubectl get deploy trino-worker
NAME           READY   UP-TO-DATE   AVAILABLE   AGE
trino-worker   1/1     1            1           29s
```

```text
$ kubectl get pods -l app=worker
NAME                            READY   STATUS    RESTARTS   AGE
trino-worker-86c4fbccff-zfk5m   1/1     Running   0          39s
```

```text
$ kubectl get rs -l app=worker
NAME                      DESIRED   CURRENT   READY   AGE
trino-worker-86c4fbccff   1         1         1       47s
```

Observe the logs using `kubectl logs` command (possibly with `-f` option).

```text
kubectl logs -f trino-worker-86c4fbccff-7l6qq
```

You should see logs similar to the following:

```text
2021-01-01T15:07:09.731Z	INFO	main	io.prestosql.security.AccessControlManager	Using system access control default
2021-01-01T15:07:09.759Z	INFO	main	io.prestosql.server.Server	======== SERVER STARTED ========
```

Should you want to log in to a pod (e.g. for debugging purposes), use `kubectl exec` command.

```text
kubectl exec -it trino-worker-86c4fbccff-7l6qq -- bash
```

Verify the number of workers in the UI at http://localhost:8080/ui/.

## Scaling Workers Up and Down

Use the UI at http://localhost:8080/ui/ to monitor the number of trino workers.

Let's wind the one worker down.

```text
kubectl scale deployment trino-worker --replicas 0
```

```text
$ kubectl get pods -l app=worker
NAME                            READY   STATUS        RESTARTS   AGE
trino-worker-86c4fbccff-hkbdr   0/1     Terminating   0          2m10s
```

```text
kubectl scale deployment trino-worker --replicas 3
```

Give it a minute or two and check out the UI at http://localhost:8080/ui/ and/or using `kubectl get pods` command.

```text
$ kubectl get pods -l app=worker
NAME                            READY   STATUS    RESTARTS   AGE
trino-worker-86c4fbccff-2xh5s   1/1     Running   0          70s
trino-worker-86c4fbccff-8kbz5   1/1     Running   0          70s
trino-worker-86c4fbccff-vl9ns   1/1     Running   0          70s
```

## Cleaning Up

In the end, clean up `minikube`. Delete the deployments.

```text
kubectl delete deployments --all
```

```text
kubectl delete services trino-clusterip trino-coordinator
```

### Hard Reset

In order to clean up the environment fully (so next time you can start from scratch) `minikube delete` should make it.

```text
minikube delete
```
