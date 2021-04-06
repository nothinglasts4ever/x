package com.github.nl4.x.b

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer

@SpringBootApplication
@EnableResourceServer
class BApplication

fun main(args: Array<String>) {
    runApplication<BApplication>(*args)
}