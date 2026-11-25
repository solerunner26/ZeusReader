package com.solerunner26.zeusreader.ui.vocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solerunner26.zeusreader.data.LibraryRepository
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.ml.OfflinePackStatus
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ml.TranslationService
import com.solerunner26.zeusreader.vocab.ReviewGrade
import com.solerunner26.zeusreader.vocab.SpacedRepetitionScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VocabularyViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val translationService: TranslationService
) : ViewModel() {

    val words: StateFlow<List<VocabWordEntity>> = libraryRepository.observeVocabularyWords()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _packStatus = MutableStateFlow(OfflinePackStatus())
    val packStatus: StateFlow<OfflinePackStatus> = _packStatus.asStateFlow()

    private val _downloadingLang = MutableStateFlow<StudyTargetLanguage?>(null)
    val downloadingLang: StateFlow<StudyTargetLanguage?> = _downloadingLang.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        refreshPackStatus()
    }

    fun refreshPackStatus() {
        viewModelScope.launch {
            _packStatus.value = translationService.getOfflinePackStatus()
        }
    }

    fun downloadPack(target: StudyTargetLanguage) {
        viewModelScope.launch {
            _downloadingLang.value = target
            _statusMessage.value = null
            val result = translationService.downloadLanguagePack(target)
            result.fold(
                onSuccess = {
                    _statusMessage.value = "${target.displayName} offline pack ready."
                    refreshPackStatus()
                },
                onFailure = { err ->
                    _statusMessage.value = "Download failed: ${err.message ?: "check network"}"
                }
            )
            _downloadingLang.value = null
        }
    }

    fun reviewWord(word: VocabWordEntity, grade: ReviewGrade) {
        viewModelScope.launch {
            val updated = SpacedRepetitionScheduler.scheduleNext(word, grade)
            libraryRepository.saveVocabularyWord(updated)
        }
    }

    fun deleteWord(wordId: String) {
        viewModelScope.launch {
            libraryRepository.deleteVocabularyWord(wordId)
        }
    }
}
