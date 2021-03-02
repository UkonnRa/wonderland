package com.ukonnra.wonderland.doorknob.authentication

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "doorknob")
@ConstructorBinding
data class DoorKnobProperties(
  val adminUrl: String,
  val frontendUrl: String,
)
