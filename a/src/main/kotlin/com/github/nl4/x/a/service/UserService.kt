package com.github.nl4.x.a.service

import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(val bCryptPasswordEncoder: BCryptPasswordEncoder) : UserDetailsService {
    override fun loadUserByUsername(username: String) = User(username, bCryptPasswordEncoder.encode("password"), emptyList())
}