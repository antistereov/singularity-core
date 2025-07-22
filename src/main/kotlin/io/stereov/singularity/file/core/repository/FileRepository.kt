package io.stereov.singularity.file.core.repository

import io.stereov.singularity.content.common.content.repository.ContentRepository
import io.stereov.singularity.file.core.model.FileMetadataDocument

interface FileRepository : ContentRepository<FileMetadataDocument>
