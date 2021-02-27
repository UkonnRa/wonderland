package com.ukonnra.wonderland.doorknob.authentication.configurations

import com.nimbusds.jose.jwk.RSAKey
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

@Configuration
@EnableWebSecurity
class ApplicationConfiguration {
  @Bean
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
    .authorizeRequests {
      it
        .antMatchers("/css/**", "/webjars/**").permitAll()
        .anyRequest().authenticated()
    }
    .formLogin {
      it.loginPage("/login")
      it.permitAll()
    }
    .build()

  @Bean
  fun userService(): UserDetailsService = InMemoryUserDetailsManager(
    User.withUsername("user1")
      .passwordEncoder { passwordEncoder().encode(it) }
      .password("password")
      .roles("USER")
      .build()
  )

  @Bean
  fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

  @Bean
  fun jwk(): RSAKey = KeyPairGenerator.getInstance("RSA").let {
    it.initialize(2048)
    val pair = it.generateKeyPair()
    RSAKey.Builder(pair.public as RSAPublicKey)
      .privateKey(pair.private as RSAPrivateKey)
      .keyID(UUID.randomUUID().toString())
      .build()
  }
}
