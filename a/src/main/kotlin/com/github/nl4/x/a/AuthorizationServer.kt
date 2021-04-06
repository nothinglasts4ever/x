package com.github.nl4.x.a

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer

@SpringBootApplication
@EnableAuthorizationServer
class AuthorizationServer

fun main(args: Array<String>) {
    runApplication<AuthorizationServer>(*args)
}