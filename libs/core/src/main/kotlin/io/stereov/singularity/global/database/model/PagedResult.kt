package io.stereov.singularity.global.database.model

import org.bson.Document

data class PagedResult(
    val count: List<CountResult>,
    val content: List<Document>
) {

    data class CountResult(val total: Long)
}
