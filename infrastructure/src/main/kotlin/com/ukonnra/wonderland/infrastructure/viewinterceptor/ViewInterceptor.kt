package com.ukonnra.wonderland.infrastructure.viewinterceptor

import com.ukonnra.wonderland.infrastructure.Filter
import com.ukonnra.wonderland.infrastructure.Identifier
import org.springframework.stereotype.Component

@Component
interface ViewInterceptor<ID : Identifier<ID>> {
  suspend fun getAll(ids: List<ID>, filter: Filter): List<ID>
}
