package io.stereov.singularity.translate.model

import com.fasterxml.jackson.annotation.JsonCreator
import io.stereov.singularity.translate.exception.model.TranslationLanguageNotImplementedException
import java.util.*

enum class Language {
    DE, EN;

    override fun toString(): String {
        return super.toString().lowercase()
    }

    fun toLocale(): Locale {
        return Locale.of(this.toString())
    }

    companion object {
        @JsonCreator
        @JvmStatic
        fun fromString(value: String): Language {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw TranslationLanguageNotImplementedException(value)
        }
    }
}
