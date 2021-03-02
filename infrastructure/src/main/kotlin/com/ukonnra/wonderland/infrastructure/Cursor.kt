package com.ukonnra.wonderland.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import com.ukonnra.wonderland.infrastructure.error.WonderlandError
import org.apache.logging.log4j.LogManager
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = Cursor.Date::class, name = "DATE"),
  JsonSubTypes.Type(value = Cursor.Num::class, name = "NUMBER"),
  JsonSubTypes.Type(value = Cursor.Str::class, name = "STR"),
)
sealed class Cursor {
  abstract val id: String
  abstract val value: Any

  data class Date(
    @JsonProperty("id") override val id: String,
    @JsonProperty("value") override val value: Instant
  ) : Cursor() {
    constructor(id: Identifier<*>, value: Instant) : this(id.toString(), value)
  }

  data class Num(
    @JsonProperty("id") override val id: String,
    @JsonProperty("value") override val value: Number
  ) : Cursor() {
    constructor(id: Identifier<*>, value: Number) : this(id.toString(), value)
  }

  data class Str(
    @JsonProperty("id") override val id: String,
    @JsonProperty("value") override val value: String
  ) : Cursor() {
    constructor(id: Identifier<*>, value: String) : this(id.toString(), value)
  }

  companion object {
    private val LOGGER = LogManager.getLogger(Cursor::class.java)

    fun decode(mapper: ObjectMapper, value: String): Cursor = try {
      val jsonString = Base64.getUrlDecoder().decode(value).toString(StandardCharsets.UTF_8)
      mapper.readValue(jsonString, jacksonTypeRef())
    } catch (e: JsonProcessingException) {
      LOGGER.error("{}", e.message, e)
      throw WonderlandError.InvalidCursor(value)
    }
  }

  fun encode(mapper: ObjectMapper): String = Base64.getUrlEncoder().encodeToString(mapper.writeValueAsBytes(this))
}
