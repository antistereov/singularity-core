package io.stereov.singularity.global.model

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

class PageableRequest(
    @field:Parameter(description = "The page number to retrieve (0-based).", example = "0")
    val page: Int = 0,

    @field:Parameter(description = "The number of elements per page.", example = "20")
    val size: Int = 20,

    @field:ArraySchema(
        arraySchema = Schema(description = "Sorting criteria in the format: field,direction (e.g., date,desc). Can be specified multiple times."),
        schema = Schema(example = "title,asc")
    )
    val sort: List<String> = emptyList()
) {
    /**
     * Converts the validated request parameters into a Spring Data Pageable object.
     * This method correctly handles multiple 'sort' parameters and defaults to ASC.
     */
    fun toPageable(): Pageable {
        val sortOrders = sort.mapNotNull { sortString ->
            val parts = sortString.split(",")
            val property = parts[0].trim()

            // Ignore blank or invalid fields
            if (property.isBlank()) return@mapNotNull null

            // Determine direction: default to ASC if direction is missing or invalid
            val direction = if (parts.size > 1 && parts[1].trim().equals("desc", ignoreCase = true)) {
                Sort.Direction.DESC
            } else {
                Sort.Direction.ASC
            }

            return@mapNotNull Sort.Order(direction, property)
        }

        // Build the final Sort object
        val finalSort = if (sortOrders.isNotEmpty()) Sort.by(sortOrders) else Sort.unsorted()

        // Return the PageRequest (the implementation of Pageable)
        return PageRequest.of(page, size, finalSort)
    }
}