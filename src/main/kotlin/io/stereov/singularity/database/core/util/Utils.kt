package io.stereov.singularity.database.core.util

import io.stereov.singularity.database.core.model.DocumentKey

fun Collection<DocumentKey>.contains(value: String) = this.contains(DocumentKey(value))

fun DocumentKey.contains(value: String) = this.value.contains(value)

fun String.replace(old: String, new: DocumentKey) = this.replace(old, new.value)