package com.solerunner26.zeusreader.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class StudyTargetLanguage(val code: String, val displayName: String, val mlKitCode: String) {
    HINDI("hi", "Hindi (हिन्दी)", TranslateLanguage.HINDI),
    GUJARATI("gu", "Gujarati (ગુજરાતી)", TranslateLanguage.GUJARATI);

    companion object {
        fun fromCode(code: String): StudyTargetLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINDI
    }
}

data class OfflinePackStatus(
    val englishDictionaryReady: Boolean = true,
    val hindiPackDownloaded: Boolean = false,
    val gujaratiPackDownloaded: Boolean = false
) {
    val isPreparedPhone: Boolean
        get() = englishDictionaryReady && hindiPackDownloaded && gujaratiPackDownloaded
}

interface TranslationService {
    suspend fun getOfflinePackStatus(): OfflinePackStatus
    suspend fun downloadLanguagePack(target: StudyTargetLanguage): Result<Unit>
    suspend fun translateFromEnglish(text: String, target: StudyTargetLanguage): Result<String>
    suspend fun recognizeLatinText(bitmap: Bitmap): Result<String>
    suspend fun detectLanguageCode(text: String): String?
}

@Singleton
class MlKitTranslationService @Inject constructor(
    @param:ApplicationContext private val context: Context
) : TranslationService {

    private val modelManager: RemoteModelManager by lazy { RemoteModelManager.getInstance() }

    override suspend fun getOfflinePackStatus(): OfflinePackStatus = withContext(Dispatchers.IO) {
        val hiModel = TranslateRemoteModel.Builder(TranslateLanguage.HINDI).build()
        val guModel = TranslateRemoteModel.Builder(TranslateLanguage.GUJARATI).build()
        val hiReady = runCatching { isModelDownloaded(hiModel) }.getOrDefault(false)
        val guReady = runCatching { isModelDownloaded(guModel) }.getOrDefault(false)
        OfflinePackStatus(
            englishDictionaryReady = true,
            hindiPackDownloaded = hiReady,
            gujaratiPackDownloaded = guReady
        )
    }

    override suspend fun downloadLanguagePack(target: StudyTargetLanguage): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val model = TranslateRemoteModel.Builder(target.mlKitCode).build()
                val conditions = DownloadConditions.Builder().build()
                suspendCancellableCoroutine { cont ->
                    modelManager.download(model, conditions)
                        .addOnSuccessListener { if (cont.isActive) cont.resume(Result.success(Unit)) }
                        .addOnFailureListener { exc -> if (cont.isActive) cont.resume(Result.failure(exc)) }
                }.getOrThrow()
            }
        }

    override suspend fun translateFromEnglish(
        text: String,
        target: StudyTargetLanguage
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")
        runCatching {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(target.mlKitCode)
                .build()
            val translator = Translation.getClient(options)
            try {
                // Only translate using already-downloaded offline model when offline
                suspendCancellableCoroutine<String> { cont ->
                    translator.translate(text)
                        .addOnSuccessListener { out -> if (cont.isActive) cont.resume(out) }
                        .addOnFailureListener { exc ->
                            if (cont.isActive) cont.resumeWithException(exc)
                        }
                }
            } finally {
                translator.close()
            }
        }
    }

    override suspend fun recognizeLatinText(bitmap: Bitmap): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                try {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    suspendCancellableCoroutine { cont ->
                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                if (cont.isActive) cont.resume(visionText.text)
                            }
                            .addOnFailureListener { exc ->
                                if (cont.isActive) cont.resumeWithException(exc)
                            }
                    }
                } finally {
                    recognizer.close()
                }
            }
        }

    override suspend fun detectLanguageCode(text: String): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null
        runCatching {
            val identifier = LanguageIdentification.getClient()
            try {
                suspendCancellableCoroutine<String?> { cont ->
                    identifier.identifyLanguage(text)
                        .addOnSuccessListener { code ->
                            if (cont.isActive) cont.resume(code.takeIf { it != "und" })
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            } finally {
                identifier.close()
            }
        }.getOrNull()
    }

    private suspend fun isModelDownloaded(model: TranslateRemoteModel): Boolean =
        suspendCancellableCoroutine { cont ->
            modelManager.isModelDownloaded(model)
                .addOnSuccessListener { downloaded ->
                    if (cont.isActive) cont.resume(downloaded)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(false)
                }
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MlModule {
    @Binds
    @Singleton
    abstract fun bindTranslationService(impl: MlKitTranslationService): TranslationService
}
