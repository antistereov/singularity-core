package io.stereov.singularity.core.group.exception.model

import io.stereov.singularity.core.group.exception.GroupException

class GroupKeyExistsException(key: String) : GroupException("A group with key \"$key\" already exists")
