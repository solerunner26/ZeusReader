package com.solerunner26.zeusreader

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute : NavKey

@Serializable
data class PdfReaderRoute(
    val editionId: String,
    val title: String,
    val localPath: String
) : NavKey

@Serializable
data class ImageBookReaderRoute(
    val editionId: String,
    val title: String
) : NavKey

@Serializable
data object VocabularyRoute : NavKey

@Serializable
data object PrepareOfflineRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object PassagesRoute : NavKey

@Serializable
data class PassageDetailRoute(val passageId: String) : NavKey

@Serializable
data class CardStudioRoute(val passageId: String) : NavKey
