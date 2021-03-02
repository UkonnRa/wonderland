package com.ukonnra.wonderland.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnore

data class Sorting(
  val field: String,
  val isAsc: Boolean = true
) {
  @JsonIgnore
  val multiplier = if (isAsc) {
    1
  } else {
    -1
  }

  fun reversed() = copy(isAsc = !this.isAsc)
}
