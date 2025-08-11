package io.stereov.singularity.global.util

import com.github.slugify.Slugify
import org.springframework.data.mongodb.core.query.Criteria

fun getFieldContainsCriteria(field: String, substring: String): Criteria {
    val regexPattern = ".*${Regex.escape(substring)}.*"
    return Criteria.where(field).regex(regexPattern, "i")
}

fun String.toSlug(): String {
    return Slugify.builder().build().slugify(this)
}
