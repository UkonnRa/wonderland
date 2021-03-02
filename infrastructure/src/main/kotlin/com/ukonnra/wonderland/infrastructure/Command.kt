package com.ukonnra.wonderland.infrastructure

interface Command<ID : Identifier<ID>> {
  val targetId: ID?
}

interface CommandHandler<ID : Identifier<ID>, C : Command<ID>, E : Event<ID>> {
  fun handleCommand(command: C): E
}
