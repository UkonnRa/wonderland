package com.ukonnra.wonderland.doorknob.authentication

import com.ukonnra.wonderland.infrastructure.error.ExternalError
import com.ukonnra.wonderland.infrastructure.error.WonderlandError
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.MonoSink
import sh.ory.hydra.ApiCallback
import sh.ory.hydra.ApiClient
import sh.ory.hydra.ApiException
import sh.ory.hydra.api.AdminApi
import sh.ory.hydra.model.AcceptConsentRequest
import sh.ory.hydra.model.AcceptLoginRequest
import sh.ory.hydra.model.CompletedRequest
import sh.ory.hydra.model.ConsentRequest
import sh.ory.hydra.model.ConsentRequestSession
import sh.ory.hydra.model.LoginRequest
import sh.ory.hydra.model.OAuth2Client
import sh.ory.hydra.model.RejectRequest
import java.net.URI

@Service
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
class AuthenticationService @Autowired constructor(
  props: DoorKnobProperties,
  private val userClient: DoorKnobUserExternal,
) {
  private final val admin: AdminApi
  private final val frontUrl: URI

  companion object {
    private val LOG = LogFactory.getLog(AuthenticationService::class.java)
    private const val REMEMBER_FOR = 3600L
    private const val USER_TYPE = "doorknob:user"
  }

  init {
    val client = ApiClient().setBasePath(props.adminUrl)
    admin = AdminApi(client)
    frontUrl = URI.create(props.frontendUrl)
  }

  fun createClients(): Mono<List<OAuth2Client>> {
    return toMono<Void> { admin.deleteOAuth2ClientAsync("absolem", it) }
      .then(toMono<Void> { admin.deleteOAuth2ClientAsync("absolem-ui", it) })
      .then(
        toMono<OAuth2Client> {
          val client = OAuth2Client()
            .clientId("absolem")
            .clientName("Absolem Backend Client")
            .clientSecret("test-secret")
            .grantTypes(listOf("client_credentials"))
            .responseTypes(listOf("code"))
            .scope("openid offline")
          admin.createOAuth2ClientAsync(client, it)
        }
      )
      .then(
        toMono<OAuth2Client> {
          val client = OAuth2Client()
            .clientId("absolem-ui")
            .clientName("Absolem Frontend Client")
            .redirectUris(listOf("http://127.0.0.1:3000/callback"))
            .grantTypes(listOf("authorization_code", "refresh_token"))
            .responseTypes(listOf("code", "token", "id_token"))
            .scope("openid offline profile:read email")
            .tokenEndpointAuthMethod("none")
          admin.createOAuth2ClientAsync(client, it)
        }
      )
      .then(
        Flux.fromIterable(
          listOf<Mono<OAuth2Client>>(
            toMono { admin.getOAuth2ClientAsync("absolem", it) },
            toMono { admin.getOAuth2ClientAsync("absolem-ui", it) }
          ),
        )
          .flatMap { it }
          .collectList()
      )
  }

  fun preLogin(challenge: String, csrfToken: CsrfToken): Mono<ResponseEntity<PreLoginModel>> =
    toMono<LoginRequest> { admin.getLoginRequestAsync(challenge, it) }
      .flatMap { req ->
        if (req.skip) {
          toMono<CompletedRequest> {
            admin.acceptLoginRequestAsync(challenge, AcceptLoginRequest().subject(req.subject), it)
          }
            .map {
              ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .location(URI.create(it.redirectTo))
                .build()
            }
        } else {
          Mono.just(
            ResponseEntity.ok()
              .contentType(MediaType.APPLICATION_JSON)
              .body(PreLoginModel(csrfToken.token, challenge, req.oidcContext?.loginHint?.toError()))
          )
        }
      }

  fun login(body: LoginModel, csrfToken: CsrfToken): Mono<ResponseEntity<PreLoginModel>> {
    return userClient.getByIdentifier(body.identifier)
      .flatMap { user ->
        val digest = DigestUtils.md5DigestAsHex(body.password.toByteArray())

        when {
          user == null -> {
            Mono.just(
              ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                  PreLoginModel(
                    csrfToken.token,
                    body.challenge,
                    WonderlandError.NotFound(USER_TYPE, body.identifier)
                  )
                )
            )
          }
          user.passwordDigest != digest -> {
            Mono.just(
              ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(PreLoginModel(csrfToken.token, body.challenge, WonderlandError.NoAuth(user.id.toString())))
            )
          }
          else -> {
            toMono<LoginRequest> { admin.getLoginRequestAsync(body.challenge, it) }
              .flatMap {
                toMono<CompletedRequest> {
                  (
                    admin.acceptLoginRequestAsync(
                      body.challenge,
                      AcceptLoginRequest().subject(user.id).remember(body.remember)
                        .rememberFor(REMEMBER_FOR).acr("0"),
                      it
                    )
                    )
                }
              }
              .map {
                ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                  .location(URI.create(it.redirectTo))
                  .build()
              }
          }
        }
      }
  }

  fun preConsent(challenge: String, csrf: CsrfToken): Mono<ResponseEntity<PreConsentModel>> {
    return toMono<ConsentRequest> { admin.getConsentRequestAsync(challenge, it) }
      .flatMap { req ->
        if (req.skip == true) {
          toMono<CompletedRequest> {
            admin.acceptConsentRequestAsync(
              challenge,
              AcceptConsentRequest()
                .grantScope(req.requestedScope)
                .grantAccessTokenAudience(req.requestedAccessTokenAudience),
              it
            )
          }.map {
            ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
              .location(URI.create(it.redirectTo))
              .build()
          }
        } else {
          Mono.just(
            ResponseEntity.ok()
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                PreConsentModel(
                  csrf.token,
                  challenge,
                  req.requestedScope!!,
                  req.subject!!,
                  req.client?.clientId!!,
                )
              )
          )
        }
      }
  }

  fun consent(body: ConsentModel): Mono<ResponseEntity<Unit>> {
    LOG.info("Get consent model: $body")
    if (!body.accept) {
      return toMono<CompletedRequest> {
        admin.rejectConsentRequestAsync(
          body.challenge,
          RejectRequest().error("access_denied").errorDescription("The resource owner denied the request"),
          it
        )
      }
        .map {
          ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(URI.create(it.redirectTo)).build()
        }
    }

    return userClient.getByIdentifier(body.user).flatMap { user ->
      when (user) {
        null -> Mono.error(WonderlandError.NotFound(USER_TYPE, body.user))
        else -> toMono<CompletedRequest> {
          admin.acceptConsentRequestAsync(
            body.challenge,
            AcceptConsentRequest()
              .grantScope(body.grantScopes)
              .session(ConsentRequestSession())
              .grantAccessTokenAudience(body.requestedAccessTokenAudience)
              .remember(body.remember)
              .rememberFor(REMEMBER_FOR),
            it
          )
        }
          .map {
            ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(URI.create(it.redirectTo)).build()
          }
      }
    }
  }
}

@Suppress("EmptyFunctionBlock")
private inline fun <reified T> toMono(crossinline fn: (ApiCallback<T>) -> Unit): Mono<T> {
  return Mono.create { sink: MonoSink<T> ->
    try {
      fn(
        object : ApiCallback<T> {
          override fun onFailure(e: ApiException, i: Int, map: Map<String, List<String>>) {
            sink.error(e)
          }

          override fun onSuccess(t: T, i: Int, map: Map<String, List<String>>) {
            sink.success(t)
          }

          override fun onUploadProgress(l: Long, l1: Long, b: Boolean) {}
          override fun onDownloadProgress(l: Long, l1: Long, b: Boolean) {}
        })
    } catch (e: ApiException) {
      sink.error(e)
    }
  }
}

private fun String.toError() = ExternalError("Hydra Error", message = this)
