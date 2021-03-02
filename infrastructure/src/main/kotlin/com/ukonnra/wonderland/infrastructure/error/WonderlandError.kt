package com.ukonnra.wonderland.infrastructure.error

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.ukonnra.wonderland.infrastructure.FilterItem
import org.springframework.http.HttpStatus

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
sealed class WonderlandError(override val message: String) : AbstractError(message) {
  data class NotFound(val type: String, val id: String) :
    WonderlandError("$type[$id] is not found") {
    override val statusCode: HttpStatus
      get() = HttpStatus.NOT_FOUND
  }

  data class AlreadyDeleted(val type: String, val id: String) : WonderlandError("$type[$id] already deleted")

  data class AlreadyExists(val type: String, val id: String) :
    WonderlandError("$type[$id] already exists") {
    override val statusCode: HttpStatus
      get() = HttpStatus.CONFLICT
  }

  data class NonNegativeParam(val paramName: String) : WonderlandError("`$paramName` should not be negative")

  data class FieldNotSortable(val type: String, val field: String) :
    WonderlandError("`$field` on $type is not sortable") {
    override val statusCode: HttpStatus
      get() = HttpStatus.FORBIDDEN
  }

  data class InvalidFilterItem(val type: String, val filterItem: FilterItem) :
    WonderlandError("$type cannot handle the filter item: $filterItem") {
    override val statusCode: HttpStatus
      get() = HttpStatus.FORBIDDEN
  }

  data class NoAuth(val userId: String? = null) :
    WonderlandError("Invalid identifier or password for User[$userId]") {
    override val statusCode: HttpStatus
      get() = HttpStatus.UNAUTHORIZED
  }

  data class UpdateNothing(val type: String, val id: String) :
    WonderlandError("$type[$id] update nothing, the update command cannot be empty")

  data class InvalidCursor(val cursor: String) :
    WonderlandError("Cursor[$cursor] is not a valid cursor")
}
