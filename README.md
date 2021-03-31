## Release Notes
* 0.0.1 - Create multi-project using Gradle

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