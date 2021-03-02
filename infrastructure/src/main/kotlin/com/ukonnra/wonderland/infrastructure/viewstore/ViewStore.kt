package com.ukonnra.wonderland.infrastructure.viewstore

import com.ukonnra.wonderland.infrastructure.Aggregate
import com.ukonnra.wonderland.infrastructure.Cursor
import com.ukonnra.wonderland.infrastructure.Filter
import com.ukonnra.wonderland.infrastructure.FilterItem
import com.ukonnra.wonderland.infrastructure.FilterOp
import com.ukonnra.wonderland.infrastructure.Identifier
import com.ukonnra.wonderland.infrastructure.QueryMode
import com.ukonnra.wonderland.infrastructure.Sorting
import com.ukonnra.wonderland.infrastructure.error.WonderlandError
import com.ukonnra.wonderland.infrastructure.viewinterceptor.ViewInterceptor

private const val DEFAULT_SIZE = 5
private const val MAX_SIZE = 1000

interface ViewStore<A : Aggregate<ID>, ID : Identifier<ID>> {
  val filterableFields: Map<String, Set<FilterOp>>
  val sortableFields: Set<String>
  val aggregateType: String

  val viewInterceptors: List<ViewInterceptor<ID>>

  suspend fun doGetAll(filterObject: Map<String, Any>, cursorField: String): List<CursorEdge<A, ID>>
  suspend fun constructFilterObject(
    queryMode: QueryMode,
    filter: Filter,
    sorting: Sorting,
    size: Int,
    isAfter: Boolean,
    cursor: Cursor?
  ): Map<String, Any>
  suspend fun checkQueryMode(queryMode: QueryMode)

  suspend fun getById(queryMode: QueryMode, id: ID): A?

  fun Sorting.checkSorting() {
    if (!sortableFields.contains(field)) {
      throw WonderlandError.FieldNotSortable(aggregateType, field)
    }
  }

  fun checkFilter(filter: Filter, queryMode: QueryMode) {
    for (f in filter) {
      if (f is FilterItem.Fulltext && f.field == null) {
        return
      }
      if (filterableFields[f.field]?.contains(f.op) != true) {
        throw WonderlandError.InvalidFilterItem(aggregateType, f)
      }
    }
  }

  private fun List<CursorEdge<A, ID>>.toConnection(
    isAfter: Boolean,
    hasLast: Boolean,
    size: Int
  ): CursorConnection<A, ID> {
    val hasNext = this.size > size
    val data = this.subList(0, size.coerceAtMost(this.size))
    return if (isAfter) {
      CursorConnection(data, hasPreviousPage = hasLast, hasNextPage = hasNext)
    } else {
      CursorConnection(
        data.reversed(),
        hasPreviousPage = hasNext,
        hasNextPage = hasLast
      )
    }
  }

  suspend fun getAllWithInterceptors(
    queryMode: QueryMode,
    filter: Filter,
    sorting: Sorting,
    size: Int,
    isAfter: Boolean,
    cursor: Cursor?
  ): List<CursorEdge<A, ID>> {
    var nextCursor = cursor
    val result = mutableListOf<CursorEdge<A, ID>>()
    while (result.size <= size) {
      val step = ((size - result.size) * 2).coerceAtLeast(size)
      val filterObj = constructFilterObject(queryMode, filter, sorting, step, isAfter, nextCursor)
      val edges = doGetAll(filterObj, sorting.field)
      if (edges.isEmpty()) {
        break
      }
      var ids = edges.map { it.node.id }
      for (interceptor in viewInterceptors) {
        ids = interceptor.getAll(ids, filter)
        if (ids.isEmpty()) {
          break
        }
      }
      result.addAll(edges.filter { ids.contains(it.node.id) })
      nextCursor = edges.lastOrNull()?.cursor
    }
    return result
  }

  @Suppress("LongParameterList")
  suspend fun getAll(
    queryMode: QueryMode,
    filter: Filter,
    sorting: Sorting,
    before: Cursor? = null,
    after: Cursor? = null,
    first: Int? = null,
    last: Int? = null
  ): CursorConnection<A, ID> {
    checkQueryMode(queryMode)
    checkFilter(filter, queryMode)
    sorting.checkSorting()

    if (first != null && first < 0) {
      throw WonderlandError.NonNegativeParam("first")
    }
    if (last != null && last < 0) {
      throw WonderlandError.NonNegativeParam("last")
    }
    val (cursor, size, isAfter) = if (after != null || first != null) {
      Triple(after, first ?: DEFAULT_SIZE, true)
    } else if (before != null || last != null) {
      Triple(before, last ?: DEFAULT_SIZE, false)
    } else {
      Triple(null, DEFAULT_SIZE, true)
    }

    val realSorting = if (isAfter) {
      sorting
    } else {
      sorting.reversed()
    }

    val result =
      getAllWithInterceptors(queryMode, filter, realSorting, size.coerceAtMost(MAX_SIZE), isAfter, cursor)
    val hasLast = if (result.isEmpty()) {
      false
    } else {
      val lastItems = getAllWithInterceptors(
        queryMode,
        filter,
        realSorting.reversed(),
        size,
        isAfter,
        result.first().cursor
      )
      lastItems.isNotEmpty()
    }

    return result.toConnection(isAfter, hasLast, size)
  }
}
