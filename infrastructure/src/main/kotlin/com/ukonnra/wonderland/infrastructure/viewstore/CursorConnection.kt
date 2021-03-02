package com.ukonnra.wonderland.infrastructure.viewstore

import com.ukonnra.wonderland.infrastructure.Aggregate
import com.ukonnra.wonderland.infrastructure.Identifier

data class CursorConnection<A : Aggregate<ID>, ID : Identifier<ID>>(
  val data: List<CursorEdge<A, ID>>,
  val hasPreviousPage: Boolean,
  val hasNextPage: Boolean
)
