package com.ukonnra.wonderland.doorknob.authentication.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HelloWorldController {
  @GetMapping("/login")
  fun index(model: Model): String {
    model.addAttribute("action", "/login")
    return "index"
  }
}
