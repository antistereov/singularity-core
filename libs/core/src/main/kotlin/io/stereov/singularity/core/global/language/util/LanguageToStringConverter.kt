package io.stereov.singularity.core.global.language.util

import io.stereov.singularity.core.global.language.model.Language
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.WritingConverter

@WritingConverter
class LanguageToStringConverter : Converter<Language, String> {
    override fun convert(source: Language): String {
        return source.toString()
    }
}
