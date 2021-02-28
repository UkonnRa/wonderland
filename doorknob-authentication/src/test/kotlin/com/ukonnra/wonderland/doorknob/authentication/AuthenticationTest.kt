package com.ukonnra.wonderland.doorknob.authentication

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class AuthenticationTest @Autowired constructor(val mvc: MockMvc) {
  @Test
  fun loginUser_user1() {
    mvc.perform(
      MockMvcRequestBuilders.get("/")
        .with(SecurityMockMvcRequestPostProcessors.user("user1"))
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
  }

  @Test
  fun loginUser_noUser() {
    mvc.perform(
      MockMvcRequestBuilders.get("/")
        .with(SecurityMockMvcRequestPostProcessors.user("noUser"))
    )
      .andExpect(MockMvcResultMatchers.status().isOk)
  }
}
