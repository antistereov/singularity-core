package io.stereov.singularity.group.exception.model

import io.stereov.singularity.group.exception.GroupException

class GroupKeyExistsException(key: String) : GroupException("A group with key \"$key\" already exists")
