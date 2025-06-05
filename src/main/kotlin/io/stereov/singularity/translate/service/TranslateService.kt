package io.stereov.singularity.translate.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.translate.model.Language
import io.stereov.singularity.translate.model.TranslateKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class TranslateService {

    private val logger = KotlinLogging.logger {}

    suspend fun translate(
        translateKey: TranslateKey,
        resource: String,
        lang: Language
    ): String = withContext(Dispatchers.IO) {
        logger.debug { "Translating key \"${translateKey.key}\" to $lang using resource: \"$resource\"" }

        return@withContext try {
            val bundle = ResourceBundle.getBundle(resource, lang.toLocale())
            bundle.getString(translateKey.key).trim()
        } catch (_: Throwable) {
            val bundle = ResourceBundle.getBundle(resource, Language.EN.toLocale())
            bundle.getString(translateKey.key).trim()
        } catch (_: Throwable) {
            return@withContext translateKey.key
        }
    }
}
