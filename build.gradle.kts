plugins {
    id("org.springframework.boot") version "2.4.4" apply false
    kotlin("jvm") version "1.4.31" apply false
    kotlin("plugin.spring") version "1.4.31" apply false
}

subprojects {
}

extra["appGroup"] = "com.github.nl4"
extra["aAppVersion"] = "0.0.3"
extra["bAppVersion"] = "0.0.3"

extra["springBootVersion"] = "2.4.4"
