package com.ukonnra.wonderland.infrastructure

interface Identifier<ID : Identifier<ID>> : Comparable<ID>

interface Aggregate<ID : Identifier<ID>> {
  val id: ID
  val companion: AggregateCompanion
}

interface AggregateCompanion {
  val type: String
}
