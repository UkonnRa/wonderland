package com.ukonnra.wonderland.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class FilterOp {
  EQ, ARRAY, RANGE, FULLTEXT
}

enum class ArrayFilterType {
  ANY, ALL
}

enum class RangeFilterType {
  GT, GTE, LT, LTE
}

data class ArrayFilterValue(
  val type: ArrayFilterType,
  val value: List<Any>
)

data class RangeFilterValue(
  val type: RangeFilterType,
  val value: Any
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "op")
@JsonSubTypes(
  JsonSubTypes.Type(value = FilterItem.Eq::class, name = "EQ"),
  JsonSubTypes.Type(value = FilterItem.Array::class, name = "ARRAY"),
  JsonSubTypes.Type(value = FilterItem.Range::class, name = "RANGE"),
  JsonSubTypes.Type(value = FilterItem.Fulltext::class, name = "FULLTEXT"),
)
sealed class FilterItem {
  abstract val op: FilterOp
  abstract val field: String?
  abstract val value: Any

  data class Eq(
    @JsonProperty("field") override val field: String,
    @JsonProperty("value") override val value: Any
  ) : FilterItem() {
    override val op: FilterOp = FilterOp.EQ
  }

  data class Array(
    @JsonProperty("field") override val field: String,
    @JsonProperty("value") override val value: ArrayFilterValue
  ) : FilterItem() {
    override val op: FilterOp = FilterOp.ARRAY
  }

  data class Range(
    @JsonProperty("field") override val field: String,
    @JsonProperty("value") override val value: RangeFilterValue
  ) : FilterItem() {
    override val op: FilterOp = FilterOp.RANGE
  }

  data class Fulltext(
    @JsonProperty("field") override val field: String?,
    @JsonProperty("value") override val value: String
  ) : FilterItem() {
    override val op: FilterOp = FilterOp.FULLTEXT
  }
}

typealias Filter = List<FilterItem>

enum class QueryMode {
  ADMIN, USER, NO_LOGIN,
}
