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
import androidx.compose.ui.unit.sp
import com.solerunner26.zeusreader.R

object ZeusFonts {
    val AtkinsonHyperlegible = FontFamily(
        Font(R.font.atkinson_hyperlegible_regular, FontWeight.Normal)
    )
    val Literata = FontFamily(
        Font(R.font.literata_regular, FontWeight.Normal)
    )
    val Fraunces = FontFamily(
        Font(R.font.fraunces_regular, FontWeight.Normal, FontStyle.Normal),
        Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic)
    )
    val NotoSansDevanagari = FontFamily(
        Font(R.font.noto_sans_devanagari_regular, FontWeight.Normal)
    )
    val NotoSansGujarati = FontFamily(
        Font(R.font.noto_sans_gujarati_regular, FontWeight.Normal)
    )
    val NotoSerifDevanagari = FontFamily(
        Font(R.font.noto_serif_devanagari_regular, FontWeight.Normal)
    )
    val NotoSerifGujarati = FontFamily(
        Font(R.font.noto_serif_gujarati_regular, FontWeight.Normal)
    )
}

/**
 * Applies DESIGN.md v2 §4 Indic typography rules so Devanagari/Gujarati matras and vowel marks
 * never clip at 100% or 200% font scale.
 */
@Suppress("DEPRECATION")
fun indicTextStyle(
    base: TextStyle,
    languageTag: String,
    useSerifForCards: Boolean = false
): TextStyle {
    val (family, multiplier) = when (languageTag.lowercase()) {
        "hi" -> (if (useSerifForCards) ZeusFonts.NotoSerifDevanagari else ZeusFonts.NotoSansDevanagari) to 1.85f
        "gu" -> (if (useSerifForCards) ZeusFonts.NotoSerifGujarati else ZeusFonts.NotoSansGujarati) to 1.80f
        else -> (if (useSerifForCards) ZeusFonts.Fraunces else ZeusFonts.Literata) to 1.65f
    }
    val fontSize = if (base.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) base.fontSize else 16.sp
    return base.copy(
        fontFamily = family,
        lineHeight = fontSize * multiplier,
        localeList = LocaleList(languageTag),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )
}

val ZeusTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = ZeusFonts.Fraunces,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = ZeusFonts.Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = ZeusFonts.Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = ZeusFonts.AtkinsonHyperlegible,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ZeusFonts.AtkinsonHyperlegible,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ZeusFonts.AtkinsonHyperlegible,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ZeusFonts.AtkinsonHyperlegible,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ZeusFonts.AtkinsonHyperlegible,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
