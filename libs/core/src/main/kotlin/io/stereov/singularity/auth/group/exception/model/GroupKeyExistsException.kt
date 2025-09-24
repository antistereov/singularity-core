package io.stereov.singularity.auth.group.exception.model

import io.stereov.singularity.auth.group.exception.GroupException

class GroupKeyExistsException(key: String) : GroupException("A group with key \"$key\" already exists")
