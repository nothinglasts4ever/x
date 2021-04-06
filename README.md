## Release Notes

### Application A (Auth Server)

* 0.0.1 - Create multi-project using Gradle
* 0.0.2 - Deploy app to k8s (Minikube locally)
* 0.0.3 - Convert to OAuth2 authorization server

### Application B

* 0.0.1 - Create multi-project using Gradle
* 0.0.2 - Deploy app to k8s (Minikube locally)
* 0.0.3 - Convert to OAuth2 resource server

## Implementation Details

### 0.0.1 - Create multi-project using Gradle

**NB**<br>
> Typically, one repo for all the services is not used in microservice architecture development.<br>
But I want to put all of them into a single repository as this is a pet project and also because I want to reuse some config files and have them in a single place

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
    jcenter()
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

### 0.0.2 - Deploy apps to k8s (Minikube locally)

Next we need to introduce Kubernetes like I did in my [another project Kuber](https://github.com/nothinglasts4ever/kuber) <br>
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
You can do the same deployment for b-app, just change exposed port to 8081 for it (as we configured in application.yaml)
<details>
  <summary>To run it locally we use Minikube</summary>

You need to install Minikube and run it using Docker driver.<br>
Next we need to switch to docker environment using `eval $(minikube docker-env)` and deploy our service using our deployment `kubectl create -f .k8s/a-deployment.yaml` <br>
To create a tunnel for NodePort use `minikube service a-app` command, it shows you URL.<br>
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

### 0.0.3 - Create OAuth2 authorization server

<details>
  <summary>To add OAuth2 functionality we need to add the following dependency to both auth and resource servers:</summary>

```kotlin
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("org.springframework.security.oauth.boot:spring-security-oauth2-autoconfigure:${property("springBootVersion")}")
```

Where `springBootVersion` is variable from root `build.gradle.kts` file:

```kotlin
extra["springBootVersion"] = "2.4.4"
```

</details>

<details>
  <summary>Next we convert a-app service to authorization server:</summary>

Add `@EnableAuthorizationServer` annotation to Spring Boot main class:

```kotlin
@SpringBootApplication
@EnableAuthorizationServer
class AuthorizationServer

fun main(args: Array<String>) {
    runApplication<AuthorizationServer>(*args)
}
```

Also need to implement `UserDetailsService` interface (for now, we simply grant access to any users with hardcoded password):

```kotlin
@Service
class UserService(val bCryptPasswordEncoder: BCryptPasswordEncoder) : UserDetailsService {
    override fun loadUserByUsername(username: String) = User(username, bCryptPasswordEncoder.encode("password"), emptyList())
}
```

Additional configuration is needed in order to inject the encoder:

```kotlin
@Configuration
class SecurityConfig {
    @Bean
    fun bCryptPasswordEncoder() = BCryptPasswordEncoder()
}
```

</details>

<details>
  <summary>And make b-app a resource server:</summary>

Add `@EnableResourceServer` annotation to Spring Boot main class:

```kotlin
@SpringBootApplication
@EnableResourceServer
class BApplication

fun main(args: Array<String>) {
    runApplication<BApplication>(*args)
}
```

</details>

<details>
  <summary>Also need to add configuration to both auth and resource servers:</summary>

```yaml
security:
  oauth2:
    client:
      client-id: ***
      client-secret: ***
    authorization:
      jwt:
        key-value: ***
```

</details>

Now `b-app` is protected from access of unauthorized users.<br>
If you try to execute `GET http://localhost:8081/actuator/info` you will get 401 error
<details>
  <summary>To get access to the service first you need JWT token:</summary>

Run `POST client:53cr3t@localhost:8080/oauth/token` via Postman with the following parameters:

* `Content-Type` header with value `application/x-www-form-urlencoded`
* body key `scope` with value `any`
* body key `grant_type` with value `password`
* body key `username` with value `username`
* body key `password` with value `password`

Or run `curl client:53cr3t@localhost:8080/oauth/token -dgrant_type=password -dusername=username -dpassword=password -dscope=any` from command line

You will get a response like the following:

```
{
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE1OTcyODA0OTQsInVzZXJfbmFtZSI6IkR1ZmYxMjMiLCJqdGkiOiJjM2Q0YzE3YS1jMzQ5LTQ5ODUtYjYwNi00Y2U4ZTJlZjhhMTgiLCJjbGllbnRfaWQiOiJjbGllbnQiLCJzY29wZSI6WyJhbnkiXX0.wumA1DRNI2M35SXjjy4m9u2vD1-BS5GE4XGfmNLeluI",
    "token_type": "bearer",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE1OTk4MjkyOTQsInVzZXJfbmFtZSI6IkR1ZmYxMjMiLCJqdGkiOiI1MDFhYTlmYi00ZGExLTQ0YzAtODM5Mi0xNjMwOTc1MjFkM2UiLCJjbGllbnRfaWQiOiJjbGllbnQiLCJzY29wZSI6WyJhbnkiXSwiYXRpIjoiYzNkNGMxN2EtYzM0OS00OTg1LWI2MDYtNGNlOGUyZWY4YTE4In0.NtWzJAZvzjdgl6O9g4AmyfXPQI-A_lPv4x5vTMb6Dyg",
    "expires_in": 43199,
    "scope": "any",
    "jti": "c3d4c17a-c349-4985-b606-4ce8e2ef8a18"
}
```

We are interested in `access_token` field
</details>

<details>
  <summary>And we need to use it in the subsequent requests:</summary>
Put it to Authorization field with Bearer type.<br>
The header of requests will look like:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjE1OTcyODA0OTQsInVzZXJfbmFtZSI6IkR1ZmYxMjMiLCJqdGkiOiJjM2Q0YzE3YS1jMzQ5LTQ5ODUtYjYwNi00Y2U4ZTJlZjhhMTgiLCJjbGllbnRfaWQiOiJjbGllbnQiLCJzY29wZSI6WyJhbnkiXX0.wumA1DRNI2M35SXjjy4m9u2vD1-BS5GE4XGfmNLeluI
```

</details>

This is exactly the same configuration as I used in my [another project OAuth](https://github.com/nothinglasts4ever/oauth) written on Java

<details>
  <summary>Last thing we need here is to put sensitive information into k8s secrets:</summary>

Replace passwords with variables in `application.yaml` of both services:

```yaml
security:
  oauth2:
    client:
      client-id: ${CLIENT_ID}
      client-secret: ${CLIENT_SECRET}
    authorization:
      jwt:
        key-value: ${SIGNING_KEY}
```

Add `auth-secret.yaml` with values encoded with base64 algorithm:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: auth-secret
data:
  CLIENT_ID: Y2xpZW50
  CLIENT_SECRET: NTNjcjN0
  SIGNING_KEY: czFnbjFuZ0szeQ==
```

And add new lines to `run.sh` script:

```shell
kubectl delete auth-secret
kubectl apply -f .k8s/auth-secret.yaml
```

</details>