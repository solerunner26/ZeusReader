package com.solerunner26.zeusreader.ui.passages

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solerunner26.zeusreader.R
import com.solerunner26.zeusreader.ui.components.SecondaryButton
import com.solerunner26.zeusreader.ui.components.SectionCard
import com.solerunner26.zeusreader.ui.components.Eyebrow
import com.solerunner26.zeusreader.ui.components.PrimaryButton
import com.solerunner26.zeusreader.ui.components.ZeusChip
import com.solerunner26.zeusreader.ui.components.ZeusSegmented
import com.solerunner26.zeusreader.ui.components.ZeusSnackbarHost
import com.solerunner26.zeusreader.ui.components.ZeusTopBar
import com.solerunner26.zeusreader.ui.components.rememberNavBarClearance
import com.solerunner26.zeusreader.ui.theme.LocalZeusPalette
import com.solerunner26.zeusreader.ui.theme.ZeusPalette
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusPack
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.contrastRatio
import com.solerunner26.zeusreader.ui.theme.hslColor
import com.solerunner26.zeusreader.ui.theme.neonBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

enum class CardStyle(val label: String) {
    COVER_BLUR("Cover blur"),
    INK("Aurora"),
    PAPER("Sticker")
}

enum class CardSize(val label: String, val widthPx: Int, val heightPx: Int) {
    SQUARE_1_1("1:1", 1080, 1080),
    PORTRAIT_4_5("4:5", 1080, 1350),
    STORY_9_16("9:16", 1080, 1920)
}

enum class CardFormat(
    val label: String,
    val mimeType: String,
    val extension: String,
    val compressFormat: Bitmap.CompressFormat,
    val quality: Int
) {
    PNG("PNG", "image/png", "png", Bitmap.CompressFormat.PNG, 100),
    JPG("JPG", "image/jpeg", "jpg", Bitmap.CompressFormat.JPEG, 92)
}

data class ShareCardRenderResult(
    val bitmap: Bitmap,
    val needsTrimHint: Boolean,
    val effectiveTextColor: Color,
    val effectiveBackdropSample: Color
)

/**
 * Renders a full-resolution share card bitmap (1080x1080, 1080x1350, or 1080x1920)
 * strictly offline following DESIGN.md v2 §8 readability rules.
 */
fun renderShareCardBitmap(
    context: Context?,
    passageText: String,
    bookTitle: String,
    author: String,
    language: String,
    hueAngle: Float,
    style: CardStyle,
    size: CardSize,
    coverBitmap: Bitmap? = null
): ShareCardRenderResult {
    val w = size.widthPx
    val h = size.heightPx
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val effectiveStyle = if (style == CardStyle.COVER_BLUR && coverBitmap == null) CardStyle.INK else style
    val inkBg = hslColor(hueAngle, 0.30f, 0.11f)

    val textColor: Color
    val backdropSample: Color

    when (effectiveStyle) {
        CardStyle.COVER_BLUR -> {
            val blurred = blurDownscale(coverBitmap!!, w, h)
            canvas.drawBitmap(blurred, null, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
            val scrimPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    Color.Black.copy(alpha = 0.55f).toArgb(),
                    Color.Black.copy(alpha = 0.68f).toArgb(),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), scrimPaint)
            textColor = Color.White
            backdropSample = ZeusPalette.cardMidGrey // Worst-case white cover under 55% black scrim -> >= 4.5:1
        }

        CardStyle.INK -> {
            // Aurora: sky base gradient + two glows + 38% dark veil (DESIGN.md §7)
            val sky = ZeusPack.fromHue(hueAngle)
            val basePaint = Paint().apply {
                shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), ZeusPalette.obsidian.toArgb(), ZeusPalette.charcoal.toArgb(), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)
            val g1 = Paint().apply {
                shader = android.graphics.RadialGradient(w * 0.15f, h * 0.12f, w * 0.75f, sky.g1.copy(alpha = 0.75f).toArgb(), Color.Transparent.toArgb(), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), g1)
            val g2 = Paint().apply {
                shader = android.graphics.RadialGradient(w * 0.9f, h * 0.7f, w * 0.7f, sky.g2.copy(alpha = 0.6f).toArgb(), Color.Transparent.toArgb(), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), g2)
            canvas.drawColor(Color.Black.copy(alpha = 0.38f).toArgb())
            textColor = Color.White
            backdropSample = inkBg
        }

        CardStyle.PAPER -> {
            // Sticker: near-black ink on cream
            canvas.drawColor(ZeusPalette.stickerCream.toArgb())
            if (coverBitmap != null) {
                val topH = (h * 0.46f).toInt()
                canvas.drawBitmap(coverBitmap, null, Rect(0, 0, w, topH), Paint(Paint.FILTER_BITMAP_FLAG))
                val fadePaint = Paint().apply {
                    shader = LinearGradient(
                        0f, topH * 0.45f, 0f, topH.toFloat(),
                        Color.Transparent.toArgb(),
                        ZeusPalette.stickerCream.toArgb(),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, topH * 0.45f, w.toFloat(), topH.toFloat(), fadePaint)
            }
            textColor = ZeusPalette.stickerInk
            backdropSample = ZeusPalette.stickerCream
        }
    }

    val padding = (w * 0.086f)
    val contentWidth = (w - 2f * padding).toInt().coerceAtLeast(100)
    val quoteMarkColor = when (effectiveStyle) {
        CardStyle.COVER_BLUR -> Color.White
        CardStyle.PAPER -> ZeusPack.fromHue(hueAngle).accentInkDay
        CardStyle.INK -> ZeusPack.fromHue(hueAngle).g2
    }

    val typeface = loadCardTypeface(context, language)
    val markPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = quoteMarkColor.toArgb()
        textSize = 96f
        this.typeface = typeface
    }
    canvas.drawText("“", padding, padding + 76f, markPaint)

    val maxBodyHeight = (h - padding * 2f - 240f).toInt().coerceAtLeast(200)
    var fontSizePx = 52f
    var layout: StaticLayout
    val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        this.typeface = typeface
    }
    while (true) {
        bodyPaint.textSize = fontSizePx
        layout = StaticLayout.Builder.obtain(passageText, 0, passageText.length, bodyPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, if (language == "hi") 1.35f else 1.28f)
            .setIncludePad(false)
            .build()
        if (layout.height <= maxBodyHeight || fontSizePx <= 15f) break
        fontSizePx -= 3f
    }
    val needsTrimHint = layout.height > maxBodyHeight && fontSizePx <= 15f

    canvas.save()
    canvas.translate(padding, padding + 108f)
    layout.draw(canvas)
    canvas.restore()

    val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.copy(alpha = 0.85f).toArgb()
        textSize = 32f
        this.typeface = Typeface.DEFAULT_BOLD
    }
    val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.copy(alpha = 0.68f).toArgb()
        textSize = 26f
        this.typeface = Typeface.DEFAULT
    }
    val footerY = h - padding - 44f
    canvas.drawText(bookTitle.ifBlank { "ZeusReader" }, padding, footerY, metaPaint)
    if (author.isNotBlank()) {
        canvas.drawText(author, padding, footerY + 34f, authorPaint)
    }

    return ShareCardRenderResult(
        bitmap = bitmap,
        needsTrimHint = needsTrimHint,
        effectiveTextColor = textColor,
        effectiveBackdropSample = backdropSample
    )
}

private fun blurDownscale(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
    val smallW = (targetW / 16).coerceAtLeast(16)
    val smallH = (targetH / 16).coerceAtLeast(16)
    val downscaled = Bitmap.createScaledBitmap(source, smallW, smallH, true)
    return Bitmap.createScaledBitmap(downscaled, targetW, targetH, true)
}

private fun loadCardTypeface(context: Context?, language: String): Typeface {
    if (context == null) return Typeface.SERIF
    val resId = when (language.lowercase()) {
        "hi" -> R.font.noto_serif_devanagari_regular
        "gu" -> R.font.noto_serif_gujarati_regular
        else -> R.font.fraunces_italic
    }
    return runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull() ?: Typeface.SERIF
}

fun encodeCardBytes(bitmap: Bitmap, format: CardFormat): ByteArray {
    val out = ByteArrayOutputStream()
    bitmap.compress(format.compressFormat, format.quality, out)
    return out.toByteArray()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardStudioScreen(
    passageId: String,
    viewModel: PassagesViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val palette = LocalZeusPalette.current
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val passage by viewModel.observePassage(passageId).collectAsStateWithLifecycle(initialValue = null)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val book = uiState.books.firstOrNull { it.editionId == passage?.editionId }
    val coverPath = book?.coverPath
    val coverBitmap by produceState<Bitmap?>(initialValue = null, coverPath) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                if (coverPath.isNullOrBlank() || !File(coverPath).exists()) null else {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeFile(coverPath, opts)
                }
            }.getOrNull()
        }
    }
    val hasCover = coverBitmap != null

    var style by remember { mutableStateOf(CardStyle.INK) }
    var size by remember { mutableStateOf(CardSize.SQUARE_1_1) }
    var format by remember { mutableStateOf(CardFormat.PNG) }

    val rendered = remember(passage?.text, passage?.language, palette.hueAngle, style, size, coverBitmap, book?.title, book?.authors) {
        passage?.let { p ->
            renderShareCardBitmap(
                context = context,
                passageText = p.text,
                bookTitle = book?.title ?: p.chapterTitle ?: "ZeusReader",
                author = book?.authors.orEmpty(),
                language = p.language,
                hueAngle = palette.hueAngle,
                style = if (style == CardStyle.COVER_BLUR && !hasCover) CardStyle.INK else style,
                size = size,
                coverBitmap = coverBitmap
            )
        }
    }

    Box(modifier = modifier.fillMaxSize().statusBarsPadding().testTag("card_studio_root")) {
        Column(modifier = Modifier.fillMaxSize()) {
            ZeusTopBar(title = "Card studio", subtitle = "Turn a spark into a post", onBack = onBack)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ZeusSpacing.gutter)
                    .padding(bottom = rememberNavBarClearance())
                    .widthIn(max = ZeusSizes.contentMaxWidth),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.lg)
            ) {
                if (rendered != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (size == CardSize.STORY_9_16) ZeusSizes.coverShelfWidth / 2 else ZeusSpacing.none)
                            .aspectRatio(size.widthPx.toFloat() / size.heightPx.toFloat())
                            .neonBorder(shape = ZeusShapes.cardSmall)
                            .clip(ZeusShapes.cardSmall)
                    ) {
                        Image(
                            bitmap = rendered.bitmap.asImageBitmap(),
                            contentDescription = "Share card preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    val contrast = contrastRatio(rendered.effectiveTextColor, rendered.effectiveBackdropSample)
                    Eyebrow(text = "Readability ${String.format(java.util.Locale.US, "%.1f", contrast)}:1" + if (contrast >= 4.5) " · passes" else " · low", accent = contrast >= 4.5)
                    if (rendered.needsTrimHint) {
                        Text("This spark is long — trim it in the editor for best legibility.", style = type.caption, color = colors.accentInk)
                    }
                }

                SectionCard(title = "Style") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)) {
                        CardStyle.entries.forEach { entry ->
                            val enabled = entry != CardStyle.COVER_BLUR || hasCover
                            ZeusChip(
                                label = if (!enabled) "${entry.label} (no cover)" else entry.label,
                                selected = style == entry,
                                enabled = enabled,
                                onClick = { style = entry }
                            )
                        }
                    }
                    Eyebrow(text = "Size", accent = true)
                    ZeusSegmented(
                        options = listOf("Square", "Post 4:5", "Story 9:16"),
                        selectedIndex = CardSize.entries.indexOf(size),
                        onSelect = { size = CardSize.entries[it] }
                    )
                    Eyebrow(text = "Format", accent = true)
                    ZeusSegmented(
                        options = CardFormat.entries.map { it.label },
                        selectedIndex = CardFormat.entries.indexOf(format),
                        onSelect = { format = CardFormat.entries[it] }
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
                    SecondaryButton(
                        label = "Save",
                        icon = Icons.Rounded.Download,
                        enabled = rendered != null,
                        onClick = {
                            val bmp = rendered?.bitmap ?: return@SecondaryButton
                            scope.launch {
                                val ok = saveToGallery(context, bmp, format)
                                snackbarHostState.showSnackbar(if (ok) "Saved to Pictures/ZeusReader" else "Save failed")
                            }
                        },
                        height = ZeusSpacing.primaryButtonHeight,
                        modifier = Modifier.weight(1f)
                    )
                    PrimaryButton(
                        label = "Share",
                        icon = Icons.Rounded.IosShare,
                        enabled = rendered != null,
                        onClick = {
                            val bmp = rendered?.bitmap ?: return@PrimaryButton
                            val current = passage ?: return@PrimaryButton
                            scope.launch {
                                shareCardViaSheet(context, bmp, format)
                                viewModel.incrementShareCount(current.id)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        ZeusSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(ZeusSpacing.gutter)
                .navigationBarsPadding()
        )
    }
}

private suspend fun saveToGallery(context: Context, bitmap: Bitmap, format: CardFormat): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val filename = "zeus_card_${System.currentTimeMillis()}.${format.extension}"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, format.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ZeusReader")
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(format.compressFormat, format.quality, out)
            } ?: return@withContext false
            true
        }.getOrDefault(false)
    }

private suspend fun shareCardViaSheet(context: Context, bitmap: Bitmap, format: CardFormat) {
    val uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "share_cards").apply { mkdirs() }
        val file = File(dir, "zeus_card_${System.currentTimeMillis()}.${format.extension}")
        FileOutputStream(file).use { out ->
            bitmap.compress(format.compressFormat, format.quality, out)
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("ZeusReader Passage Card", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share passage card"))
}
