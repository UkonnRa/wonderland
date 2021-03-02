package com.ukonnra.wonderland.infrastructure.repository

import com.ukonnra.wonderland.infrastructure.Aggregate
import com.ukonnra.wonderland.infrastructure.Identifier
import reactor.core.publisher.Mono

interface Repository<A : Aggregate<ID>, ID : Identifier<ID>> {
  fun saveAll(aggregates: List<A>): Mono<Void>
  fun getById(targetId: ID): Mono<A?>
}
