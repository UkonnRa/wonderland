package com.ukonnra.wonderland.infrastructure.error

import org.springframework.http.HttpStatus

data class ExternalError(
  val code: String,
  val data: Map<String, Any> = emptyMap(),
  override val message: String = "<Unknown External Error>",
  override val statusCode: HttpStatus = HttpStatus.BAD_REQUEST,
) : AbstractError(message)
