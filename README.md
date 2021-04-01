## Release Notes

* 0.0.1 - Create multi-project using Gradle
* 0.0.2 - Deploy apps to k8s (Minikube locally)

## Implementation Details

### Create multi-project using Gradle

First we need to create multi-project using Gradle and Kotlin Script.
<details>
  <summary>It has the following structure:</summary>

* module A
    * build.gradle.kts
    * settings.gradle.kts
* module B
    * build.gradle.kts
    * settings.gradle.kts
* build.gradle.kts
* settings.gradle.kts

</details>

<details>
  <summary>This is minimal root build.gradle.kts</summary>

```kotlin
plugins {
    id("org.springframework.boot") version "2.4.4" apply false
    kotlin("jvm") version "1.4.31" apply false
    kotlin("plugin.spring") version "1.4.31" apply false
}

subprojects {
}

extra["appGroup"] = "com.github.nl4"
extra["appVersion"] = "0.0.1"
```

`apply false` is used just to declare plugin version to be reused in modules
</details> 

<details>
  <summary>... and root settings.gradle.kts</summary>

```kotlin
rootProject.name = "x"

include("a")
include("b")
```

Here we declared modules
</details>

<details>
  <summary>This is build.gradle.kts from one of the modules</summary>

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
}

group = "${property("appGroup")}"
version = "${property("appVersion")}"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
}

springBoot {
    buildInfo()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}
```

As you can see there is no need to declare plugins version in modules.<br>
Also it contains Java 11 configuration and Spring MVC dependency with Actuator.<br>
To fill Actuator endpoint with service info add the following lines:

```yaml
springBoot {
  buildInfo()
}
```

After service starts we can execute `GET http://localhost:8080/actuator/info` to see app version and make sure it works
</details>

<details>
  <summary>... and settings.gradle.kts</summary>

```kotlin
rootProject.name = "a"
```

We just declared module name
</details>

<details>
  <summary>In order to start both project we need to assign different ports to them</summary>

Default Spring server port is 8080, so we just need to set another one for another project in `application.yaml`

```yaml
server:
  port: 8081
```

</details>

### Deploy apps to k8s (Minikube locally)

Next we need to introduce Kubernetes like I did in my [another project](https://github.com/nothinglasts4ever/kuber) <br>
It describes different aspects like ConfigMap or Dockerfile but for now we just need k8s service and deployment configuration
<details>
  <summary>Let's add a-deployment.yaml file with the following content:</summary>

```yaml
apiVersion: v1
kind: Service
metadata:
  name: a-app
spec:
  selector:
    app: a-app
  ports:
    - protocol: TCP
      port: 8080
  type: NodePort
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: a-app
spec:
  selector:
    matchLabels:
      app: a-app
  replicas: 1
  template:
    metadata:
      labels:
        app: a-app
    spec:
      containers:
        - name: a-app
          image: a:0.0.2
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
```

Here we added deployment with 1 replica and container description:

```yaml
      containers:
        - name: a-app
          image: a:0.0.2
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
```

Now a-app is application `a` with version 0.0.2 which has port 8080 open.<br>
To build such artifact Gradle command `gradle bootBuildImage` is used. It creates Docker image based on our Spring Bot app configuration. E.g. it's built up on Java 11.<br>
Also we described kubernetes service which uses a-app and expose 8080 port:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: a-app
spec:
  selector:
    app: a-app
  ports:
    - protocol: TCP
      port: 8080
  type: NodePort
```

NodePort type is used in order to expose 8080 port.
</details>
You can do the same deployment for b-app, just change exposed port to 8081 for it (as we configured in `application.yaml`)
<details>
  <summary>To run it locally we use Minikube</summary>

You need to install Minikube and run it using Docker driver.<br>
Next we need to switch to docker environment using `eval $(minikube docker-env)` and deploy our service using our deployment `kubectl create -f .k8s/a-deployment.yaml` <br>
To create tunnel for NodePort use `minikube service a-app` command, it shows you URL.<br>
Use it to see that app is deployed and reachable via `GET http://ip-address:port/actuator/info` <br>
Let's create `bash` script for it:

```shell
#!/bin/bash
eval $(minikube docker-env)
gradle bootBuildImage
kubectl delete -f .k8s/a-deployment.yaml
kubectl create -f .k8s/a-deployment.yaml
minikube service a-app
```

</details>
