#!/bin/bash

eval $(minikube docker-env)
gradle bootBuildImage

kubectl delete auth-secret
kubectl apply -f .k8s/auth-secret.yaml

kubectl delete -f .k8s/a-deployment.yaml
kubectl delete -f .k8s/b-deployment.yaml

kubectl create -f .k8s/a-deployment.yaml
kubectl create -f .k8s/b-deployment.yaml

# Run in separate tabs:
# minikube service a-app
# minikube service b-app
