package com.github.nl4.x.a.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
class SecurityConfig {
    @Bean
    fun bCryptPasswordEncoder() = BCryptPasswordEncoder()
}