#!/bin/bash

eval $(minikube docker-env)
gradle bootBuildImage

kubectl delete -f .k8s/a-deployment.yaml
kubectl delete -f .k8s/b-deployment.yaml

kubectl create -f .k8s/a-deployment.yaml
kubectl create -f .k8s/b-deployment.yaml

#minikube service a-app
