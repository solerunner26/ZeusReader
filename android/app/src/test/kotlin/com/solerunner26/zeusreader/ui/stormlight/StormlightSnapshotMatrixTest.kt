package com.solerunner26.zeusreader.ui.stormlight

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.solerunner26.zeusreader.data.PassageWithTags
import com.solerunner26.zeusreader.data.local.PassageEntity
import com.solerunner26.zeusreader.data.local.PassageFolderEntity
import com.solerunner26.zeusreader.data.local.VocabWordEntity
import com.solerunner26.zeusreader.reader.prefs.AppLanguage
import com.solerunner26.zeusreader.reader.prefs.AppUiConfig
import com.solerunner26.zeusreader.reader.prefs.ReaderSettings
import com.solerunner26.zeusreader.ui.home.HomeContent
import com.solerunner26.zeusreader.ui.library.LibraryScreenContent
import com.solerunner26.zeusreader.ui.library.LibraryUiState
import com.solerunner26.zeusreader.ui.passages.PassagesContent
import com.solerunner26.zeusreader.ui.passages.PassagesUiState
import com.solerunner26.zeusreader.ui.preview.PreviewFixtures
import com.solerunner26.zeusreader.ui.reader.OrbTool
import com.solerunner26.zeusreader.ui.reader.StormlightReaderFrame
import com.solerunner26.zeusreader.ui.reader.TypographyPanelContent
import com.solerunner26.zeusreader.ui.settings.SettingsScreenContent
import com.solerunner26.zeusreader.ui.stats.ReadingStatsUiState
import com.solerunner26.zeusreader.ui.stats.StatsContent
import com.solerunner26.zeusreader.ui.theme.StormlightSky
import com.solerunner26.zeusreader.ui.theme.ZeusPaper
import com.solerunner26.zeusreader.ui.theme.ZeusSky
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.ZeusType
import com.solerunner26.zeusreader.ui.vocab.VocabularyContent
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Phase 5 Roborazzi matrix (DESIGN.md §8):
 * {Storm, Aurora, Sunset} × {Cloud (Daylight), OLED, E-ink} × fontScale {1.0, 2.0} × {EN, HI, GU}.
 *
 * - `./gradlew :app:testDebugUnitTest` (and `uiCheck`) renders every cell and checks touch targets.
 * - `./gradlew :app:recordRoborazziDebug --tests '*StormlightSnapshotMatrixTest*'` records baselines
 *   into src/test/snapshots/stormlight (only after the design is approved).
 * - `./gradlew :app:verifyRoborazziDebug` compares against them.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class StormlightSnapshotMatrixTest(
    private val sky: ZeusSky,
    private val paper: ZeusPaper,
    private val fontScale: Float,
    private val lang: AppLanguage
) {
    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-fs{2}-{3}")
        fun params(): List<Array<Any>> = buildList {
            for (sky in listOf(ZeusSky.STORM, ZeusSky.AURORA, ZeusSky.SUNSET)) {
                for (paper in listOf(ZeusPaper.CLOUD, ZeusPaper.OLED, ZeusPaper.EINK)) {
                    for (fs in listOf(1.0f, 2.0f)) {
                        for (lang in AppLanguage.entries) add(arrayOf(sky, paper, fs, lang))
                    }
                }
            }
        }
    }

    private val tag get() = "${sky.name.lowercase()}_${paper.name.lowercase()}_fs${(fontScale * 10).toInt()}_${lang.tag}"

    private fun t(en: String, hi: String, gu: String) = when (lang) {
        AppLanguage.EN -> en
        AppLanguage.HI -> hi
        AppLanguage.GU -> gu
    }

    private val books
        get() = listOf(
            PreviewFixtures.book(1, t("Soil Science for Beginners", "शुरुआती लोगों के लिए मृदा विज्ञान", "શરૂઆત કરનારાઓ માટે માટી વિજ્ઞાન"), "R. Mehta", progress = 370),
            PreviewFixtures.book(2, t("Organic Chemistry Notes", "कार्बनिक रसायन नोट्स", "કાર્બનિક રસાયણ નોંધો"), "A. Shah", format = "PDF", progress = 12),
            PreviewFixtures.book(3, t("Physics, Volume Two", "भौतिकी, खंड दो", "ભૌતિકશાસ્ત્ર, ભાગ બે"), "H. Verma", format = "PDF")
        )

    @Composable
    private fun Matrix(content: @Composable () -> Unit) {
        val base = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
            ZeusTheme(sky = sky, paper = paper, reduceMotion = true, daylight = paper == ZeusPaper.CLOUD) {
                StormlightSky(animate = false) { content() }
            }
        }
    }

    private fun capture(name: String, content: @Composable () -> Unit) {
        rule.setContent { Matrix(content) }
        rule.waitForIdle()
        assertTouchTargets(name)
        rule.onRoot().captureRoboImage("src/test/snapshots/stormlight/${name}_$tag.png")
    }

    /**
     * Every clickable node must be ≥ 36dp visually in both directions. Compose expands the hit area of
     * smaller pointer targets to the 48dp minimum touch target automatically, so 36–47dp chips are
     * still 48dp to the finger; anything under 36dp is a real problem. Sliders are excluded.
     */
    private fun assertTouchTargets(screen: String) {
        val density = rule.density.density
        val small = mutableListOf<String>()
        fun visit(node: SemanticsNode) {
            if (node.config.getOrNull(SemanticsActions.OnClick) != null && node.config.getOrNull(SemanticsActions.SetProgress) == null) {
                val w = node.size.width / density
                val h = node.size.height / density
                if ((w < 36f || h < 36f) && w > 0f && h > 0f) small += "${node.config} ${w}x$h"
            }
            node.children.forEach(::visit)
        }
        visit(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        assertTrue("$screen [$tag] has undersized touch targets:\n${small.take(5).joinToString("\n")}", small.isEmpty())
    }

    @Test
    fun home() = capture("home") {
        HomeContent(
            books = books,
            featuredSparkText = t("Every gram of healthy soil is a city of living things.", "स्वस्थ मिट्टी का हर ग्राम जीवों का एक शहर है।", "સ્વસ્થ માટીનો દરેક ગ્રામ જીવોનું એક શહેર છે."),
            streakDays = 12, totalBooks = 3, booksInProgress = 2, dueVocabWords = 6, glassEnabled = true,
            onOpenBook = {}, onOpenLibrary = {}, onOpenPassages = {}, onOpenVocab = {}, onOpenOnline = {},
            onOpenStats = {}, onOpenPrepareOffline = {}, onOpenSettings = {}
        )
    }

    @Test
    fun olympus() = capture("olympus") {
        LibraryScreenContent(
            uiState = LibraryUiState.Populated(books),
            isImporting = false,
            snackbarHostState = SnackbarHostState(),
            onImportClick = {}, onOpenWatchedFolders = {}, onSearchQueryChange = {}, onFilterSelected = {}, onOpenBook = {}
        )
    }

    @Test
    fun settings() = capture("settings") {
        SettingsScreenContent(
            config = AppUiConfig(themeHue = sky.hue, appLanguage = lang),
            darkTheme = paper != ZeusPaper.CLOUD,
            onBack = {}, onSelectAppTheme = {}, onSelectLanguage = {}, onSelectPreset = {}, onToggleHueFromCover = {},
            onAuraIntensityChange = {}, onSelectReaderSurface = {}, onToggleFocusRibbon = {}, onToggleGlassUi = {},
            onToggleAestheticElements = {}, onToggleMasterAnimations = {}
        )
    }

    @Test
    fun sparks() = capture("sparks") {
        val now = 1_700_000_000_000L
        PassagesContent(
            uiState = PassagesUiState(
                passages = listOf(
                    PassageWithTags(
                        PassageEntity(
                            id = "p1", bookId = "w1", editionId = "e1", folderId = "f1",
                            text = t("Capillary action draws water upward through narrow soil pores.", "केशिका क्रिया संकीर्ण छिद्रों से पानी ऊपर खींचती है।", "કેશિકા ક્રિયા સાંકડા છિદ્રો દ્વારા પાણી ઉપર ખેંચે છે."),
                            originalText = "", locatorJson = "", pageLabel = "p.84", chapterTitle = null, language = lang.tag,
                            note = null, createdAt = now, updatedAt = now
                        ),
                        emptyList()
                    )
                ),
                folders = listOf(PassageFolderEntity("f1", t("Soil science", "मृदा विज्ञान", "માટી વિજ્ઞાન"), 0, now, now)),
                books = books
            ),
            snackbarHostState = SnackbarHostState(),
            onSearchQueryChange = {}, onSelectFolder = {}, onSortMode = {}, onToggleSelection = {}, onClearSelection = {},
            onDeleteSelected = {}, onMoveSelected = {}, onCreateFolder = {}, onDeleteFolder = {}, onSoftDelete = {},
            onSaveManual = { _, _, _, _ -> }, onOpenPassage = {}, onCreateCard = {}
        )
    }

    @Test
    fun words() = capture("words") {
        VocabularyContent(
            words = listOf(
                VocabWordEntity(
                    id = "v1", word = "capillary", lemma = "capillary", targetLang = lang.tag.takeIf { it != "en" } ?: "hi",
                    englishMeaning = "a very fine tube", translatedMeaning = t("केशिका", "केशिका", "કેશિકા"),
                    bookTitle = books.first().title, sentenceContext = "Capillary action draws water upward.",
                    nextReviewEpochMs = 0L, createdAtEpochMs = 0L, updatedAtEpochMs = 0L
                )
            ),
            onReview = { _, _ -> },
            onDelete = {},
            nowEpochMs = 1L
        )
    }

    @Test
    fun stats() = capture("stats") {
        StatsContent(ReadingStatsUiState(3, 2, 14, 32, 6, 12, 40), onBack = {})
    }

    @Test
    fun reader() = capture("reader") {
        StormlightReaderFrame(
            title = books.first().title,
            meta = t("Ch 4 · Water in soil", "अध्याय 4 · मिट्टी में पानी", "પ્રકરણ 4 · માટીમાં પાણી"),
            paperColor = paper.bg,
            onBack = {},
            progressFraction = 0.37f,
            progressLabel = "p.84 / 312",
            tools = listOf(OrbTool("Lens", Icons.Rounded.Translate) {}),
            onPrevious = {},
            onNext = {}
        ) {
            Box(Modifier.fillMaxSize().padding(ZeusSpacing.xl)) {
                Text(
                    t(
                        "Soil is not a solid block but a maze of particles and the spaces between them.",
                        "मिट्टी कोई ठोस खंड नहीं, बल्कि कणों और उनके बीच के रिक्त स्थानों की भूलभुलैया है।",
                        "માટી કોઈ નક્કર ટુકડો નથી, પરંતુ કણો અને તેમની વચ્ચેની જગ્યાઓની ભુલભુલામણી છે."
                    ),
                    style = ZeusType.readingFor(lang.tag),
                    color = paper.ink
                )
            }
        }
    }

    @Test
    fun vibeCheck() = capture("vibe") {
        Box(Modifier.verticalScroll(rememberScrollState()).padding(ZeusSpacing.gutter)) {
            TypographyPanelContent(settings = ReaderSettings(), isPdfMode = true, onSettingsChange = {})
        }
    }
}
