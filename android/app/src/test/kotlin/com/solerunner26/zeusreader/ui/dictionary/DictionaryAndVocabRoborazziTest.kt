package com.solerunner26.zeusreader.ui.dictionary

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.solerunner26.zeusreader.dictionary.BilingualGloss
import com.solerunner26.zeusreader.dictionary.DictionaryLookupResult
import com.solerunner26.zeusreader.dictionary.DictionarySense
import com.solerunner26.zeusreader.ml.OfflinePackStatus
import com.solerunner26.zeusreader.ml.StudyTargetLanguage
import com.solerunner26.zeusreader.ui.theme.ZeusReaderTheme
import com.solerunner26.zeusreader.ui.vocab.PrepareOfflineContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w412dp-h915dp-420dpi")
class DictionaryAndVocabRoborazziTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val capillaryLookup = DictionaryLookupResult(
        queryWord = "capillary",
        matchedLemma = "capillary",
        englishSenses = listOf(
            DictionarySense("noun", "A narrow tube or pore in which liquid rises by surface tension.", "Water ascends through fine soil capillaries."),
            DictionarySense("adjective", "Relating to or resembling a hair; having a very small internal diameter.", "")
        ),
        hindiGlosses = listOf(
            BilingualGloss(StudyTargetLanguage.HINDI, "noun", "केशिका (सूक्ष्म नलिका)", "keshika", "kaikki.org")
        ),
        gujaratiGlosses = listOf(
            BilingualGloss(StudyTargetLanguage.GUJARATI, "noun", "કેશિકા (સૂક્ષ્મ નળી)", "keshika", "kaikki.org")
        ),
        attribution = "WordNet 3.1 & Wiktionary",
        buildDate = "2026-11-25",
        usedMlKitFallback = false
    )

    @Test
    fun captureDictionarySheet_hindi_100() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = false) {
                DictionarySheetContent(
                    initialWord = "capillary",
                    sentenceContext = "Capillary action draws water upward through narrow soil pores against gravity, supplying plant roots during dry intervals.",
                    lookupResult = capillaryLookup,
                    sentenceTranslation = "केशिका क्रिया गुरुत्वाकर्षण के विरुद्ध मिट्टी के संकीर्ण छिद्रों से पानी को ऊपर की ओर खींचती है।",
                    selectedTargetLang = StudyTargetLanguage.HINDI,
                    onSelectTargetLang = {},
                    onLookupWord = {},
                    onSpeak = {},
                    onSaveToVocab = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("dictionary_sheet_root")
            .captureRoboImage("dictionary_sheet_hindi_100.png")
    }

    @Test
    @Config(sdk = [35], qualifiers = "w412dp-h915dp-420dpi", fontScale = 2.0f)
    fun captureDictionarySheet_gujarati_200() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = false) {
                DictionarySheetContent(
                    initialWord = "capillary",
                    sentenceContext = "Capillary action draws water upward through narrow soil pores.",
                    lookupResult = capillaryLookup,
                    sentenceTranslation = "કેશિકા ક્રિયા ગુરુત્વાકર્ષણની વિરુદ્ધ માટીના સાંકડા છિદ્રોમાંથી પાણીને ઉપર તરફ ખેંચે છે.",
                    selectedTargetLang = StudyTargetLanguage.GUJARATI,
                    onSelectTargetLang = {},
                    onLookupWord = {},
                    onSpeak = {},
                    onSaveToVocab = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("dictionary_sheet_root")
            .captureRoboImage("dictionary_sheet_gujarati_200.png")
    }

    @Test
    fun capturePrepareOffline_freshInstall() {
        composeTestRule.setContent {
            ZeusReaderTheme(darkTheme = false) {
                PrepareOfflineContent(
                    packStatus = OfflinePackStatus(
                        englishDictionaryReady = true,
                        hindiPackDownloaded = false,
                        gujaratiPackDownloaded = false
                    ),
                    downloadingLang = null,
                    statusMessage = null,
                    onDownloadPack = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("prepare_offline_root")
            .captureRoboImage("prepare_offline_fresh_install.png")
    }
}
