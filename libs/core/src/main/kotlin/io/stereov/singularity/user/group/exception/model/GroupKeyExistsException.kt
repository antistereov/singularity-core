package io.stereov.singularity.user.group.exception.model

import io.stereov.singularity.user.group.exception.GroupException

class GroupKeyExistsException(key: String) : GroupException("A group with key \"$key\" already exists")
