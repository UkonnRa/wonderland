package com.ukonnra.wonderland.infrastructure.error

import org.springframework.http.HttpStatus

abstract class AbstractError(override val message: String) : RuntimeException(message) {
  open val statusCode: HttpStatus = HttpStatus.BAD_REQUEST
}
