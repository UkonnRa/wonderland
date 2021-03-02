package com.ukonnra.wonderland.doorknob.authentication

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import sh.ory.hydra.model.OAuth2Client

@RestController
class OryController @Autowired constructor(private val authentication: AuthenticationService) {
  @PostMapping("/clients")
  fun createClients(): Mono<List<OAuth2Client>> {
    return authentication.createClients()
  }

  @GetMapping("/login")
  fun preLogin(
    @RequestParam("login_challenge") challenge: String,
    csrf: CsrfToken
  ): Mono<ResponseEntity<PreLoginModel>> {
    return authentication.preLogin(challenge, csrf)
  }

  @PostMapping("/login")
  fun login(@RequestBody body: LoginModel, csrf: CsrfToken): Mono<ResponseEntity<PreLoginModel>> {
    return authentication.login(body, csrf)
  }

  @GetMapping("/consent")
  fun preConsent(
    @RequestParam("consent_challenge") challenge: String,
    csrf: CsrfToken
  ): Mono<ResponseEntity<PreConsentModel>> {
    return authentication.preConsent(challenge, csrf)
  }

  @PostMapping("/consent")
  fun consent(@RequestBody body: ConsentModel): Mono<ResponseEntity<Unit>> {
    return authentication.consent(body)
  }
}
