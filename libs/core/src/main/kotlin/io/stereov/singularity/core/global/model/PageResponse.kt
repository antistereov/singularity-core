package io.stereov.singularity.core.global.model

open class PageResponse<T>(
    open val content: List<T>,
    open val pageNumber: Int,
    open val pageSize: Int,
    open val numberOfElements: Int,
    open val totalElements: Long,
    open val totalPages: Int,
    open val first: Boolean,
    open val last: Boolean,
    open val hasNext: Boolean,
    open val hasPrevious: Boolean,
    open val sort: List<Sort>?,
    open val pageable: Pageable
) {

    data class Sort(
        val property: String,
        val direction: String
    )

    data class Pageable(
        val pageNumber: Int,
        val pageSize: Int,
        val sort: List<Sort>? = emptyList()
    )

    inline fun <R> map(transform: (T) -> R): PageResponse<R> {
        return PageResponse(
            content = content.map(transform),
            pageNumber = pageNumber,
            pageSize = pageSize,
            numberOfElements = numberOfElements,
            totalElements = totalElements,
            totalPages = totalPages,
            first = first,
            last = last,
            hasNext = hasNext,
            hasPrevious = hasPrevious,
            sort = sort,
            pageable = pageable
        )
    }
}
