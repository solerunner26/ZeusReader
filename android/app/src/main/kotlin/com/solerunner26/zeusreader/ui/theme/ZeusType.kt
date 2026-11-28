package com.solerunner26.zeusreader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.solerunner26.zeusreader.R

/** Sunset type scale: Plus Jakarta Sans for UI, Literata / Noto for reading. */
object ZeusType {
    val Jakarta = FontFamily(
        Font(R.font.plus_jakarta_regular, FontWeight.Normal),
        Font(R.font.plus_jakarta_medium, FontWeight.Medium),
        Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_bold, FontWeight.Bold),
        Font(R.font.plus_jakarta_extrabold, FontWeight.ExtraBold),
        Font(R.font.plus_jakarta_extrabold, FontWeight.Black)
    )
    val Literata = FontFamily(
        Font(R.font.literata_regular, FontWeight.Normal),
        Font(R.font.literata_regular, FontWeight.Normal, FontStyle.Italic)
    )
    val NotoSerifDevanagari = FontFamily(Font(R.font.noto_serif_devanagari_regular, FontWeight.Normal))
    val NotoSerifGujarati = FontFamily(Font(R.font.noto_serif_gujarati_regular, FontWeight.Normal))
    val NotoSansDevanagari = FontFamily(Font(R.font.noto_sans_devanagari_regular, FontWeight.Normal))
    val NotoSansGujarati = FontFamily(Font(R.font.noto_sans_gujarati_regular, FontWeight.Normal))

    private fun ui(size: Int, line: Int, weight: FontWeight, spacing: Float = 0f) = TextStyle(
        fontFamily = Jakarta,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = spacing.sp
    )

    val hero = ui(54, 56, FontWeight.ExtraBold, -2.4f)
    val display = ui(30, 36, FontWeight.ExtraBold, -0.9f)
    val title = ui(24, 30, FontWeight.ExtraBold, -0.7f)
    val headline = ui(17, 22, FontWeight.ExtraBold, -0.3f)
    val bodyStrong = ui(15, 21, FontWeight.Bold)
    val body = ui(14, 20, FontWeight.Medium)
    val label = ui(13, 17, FontWeight.Bold)
    val caption = ui(12, 16, FontWeight.Medium)
    val tiny = ui(10, 13, FontWeight.Bold)
    val eyebrow = ui(11, 14, FontWeight.ExtraBold, 0.9f)
    val number = ui(26, 28, FontWeight.ExtraBold, -0.8f)
    val numberSmall = ui(19, 22, FontWeight.ExtraBold, -0.4f)
    val button = ui(15, 19, FontWeight.ExtraBold)

    val reading = TextStyle(fontFamily = Literata, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 29.sp)
    val readingTitle = TextStyle(fontFamily = Literata, fontWeight = FontWeight.Normal, fontSize = 23.sp, lineHeight = 29.sp)
    val quote = TextStyle(fontFamily = Literata, fontStyle = FontStyle.Italic, fontSize = 15.sp, lineHeight = 22.sp)

    /** Reading style for a language tag (en / hi / gu) with script-appropriate line height. */
    @Suppress("DEPRECATION")
    fun readingFor(languageTag: String, base: TextStyle = reading): TextStyle {
        val (family, multiplier) = when (languageTag.lowercase()) {
            "hi" -> NotoSerifDevanagari to 1.85f
            "gu" -> NotoSerifGujarati to 1.80f
            else -> Literata to 1.70f
        }
        val size = if (base.fontSize != TextUnit.Unspecified) base.fontSize else 17.sp
        return base.copy(
            fontFamily = family,
            lineHeight = size * multiplier,
            localeList = LocaleList(languageTag),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
        )
    }

    /** UI font for Hindi/Gujarati text shown in the interface (meanings, chips). */
    fun uiFor(languageTag: String): FontFamily = when (languageTag.lowercase()) {
        "hi" -> NotoSansDevanagari
        "gu" -> NotoSansGujarati
        else -> Jakarta
    }

    /** Base reading size in sp for a reader font-size percentage (100% = 16sp). */
    fun readingSize(percent: Int): TextUnit = (16f * percent / 100f).sp

    val materialTypography = Typography(
        displayLarge = hero,
        displayMedium = display,
        displaySmall = number,
        headlineLarge = display,
        headlineMedium = title,
        headlineSmall = headline,
        titleLarge = headline,
        titleMedium = bodyStrong,
        titleSmall = label,
        bodyLarge = body.copy(fontSize = 15.sp, lineHeight = 21.sp),
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = button,
        labelMedium = label,
        labelSmall = tiny
    )
}
