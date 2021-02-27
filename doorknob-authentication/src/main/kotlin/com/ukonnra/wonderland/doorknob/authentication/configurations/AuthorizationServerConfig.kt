package com.ukonnra.wonderland.doorknob.authentication.configurations

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings
import java.util.UUID

@Configuration(proxyBeanMethods = false)
@Import(OAuth2AuthorizationServerConfiguration::class)
class AuthorizationServerConfig @Autowired constructor(val jwk: RSAKey) {
  @Bean
  fun clientRepository(): RegisteredClientRepository =
    RegisteredClient.withId(UUID.randomUUID().toString())
      .clientId("messaging-client")
      .clientSecret("secret")
      .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .redirectUri("http://localhost:8080/login/oauth2/code/messaging-client-oidc")
      .redirectUri("http://localhost:8080/authorized")
      .scope(OidcScopes.OPENID)
      .scope("message.read")
      .scope("message.write")
      .clientSettings { it.requireUserConsent(true) }
      .build()
      .let { InMemoryRegisteredClientRepository(it) }

  @Bean
  fun providerSettings(): ProviderSettings = ProviderSettings().issuer("http://localhost:9000")

  @Bean
  fun jwkSource(): JWKSource<SecurityContext> =
    JWKSource<SecurityContext> { selector, _ -> selector.select(JWKSet(jwk)) }
}
