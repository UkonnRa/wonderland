package com.ukonnra.wonderland.doorknob.authentication

import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
interface DoorKnobUserExternal {
  fun getByIdentifier(identifier: String): Mono<DoorKnobUserModel?>
}
