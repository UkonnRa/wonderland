package com.ukonnra.wonderland.infrastructure

interface Event<ID : Identifier<ID>> {
  val targetId: ID
}
