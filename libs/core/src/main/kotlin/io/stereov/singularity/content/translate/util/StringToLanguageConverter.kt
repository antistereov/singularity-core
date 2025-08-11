package io.stereov.singularity.content.translate.util

import io.stereov.singularity.content.translate.model.Language
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter

@ReadingConverter
class StringToLanguageConverter : Converter<String, Language> {
    override fun convert(source: String): Language {
        return Language.fromString(source)
    }
}
