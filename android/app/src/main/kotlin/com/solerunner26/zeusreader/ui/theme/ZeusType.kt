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

object ZeusType {
    val Unbounded = FontFamily(
        Font(R.font.unbounded, FontWeight.SemiBold),
        Font(R.font.unbounded, FontWeight.Bold),
        Font(R.font.unbounded, FontWeight.ExtraBold),
        Font(R.font.unbounded, FontWeight.Black)
    )
    val BricolageGrotesque = FontFamily(
        Font(R.font.bricolage_grotesque, FontWeight.Normal),
        Font(R.font.bricolage_grotesque, FontWeight.Medium),
        Font(R.font.bricolage_grotesque, FontWeight.SemiBold),
        Font(R.font.bricolage_grotesque, FontWeight.Bold),
        Font(R.font.bricolage_grotesque, FontWeight.ExtraBold)
    )
    val Literata = FontFamily(
        Font(R.font.literata_regular, FontWeight.Normal),
        Font(R.font.literata_regular, FontWeight.Normal, FontStyle.Italic)
    )
    val JetBrainsMono = FontFamily(
        Font(R.font.jetbrains_mono, FontWeight.Normal),
        Font(R.font.jetbrains_mono, FontWeight.SemiBold)
    )
    val Baloo2 = FontFamily(
        Font(R.font.baloo2, FontWeight.Normal),
        Font(R.font.baloo2, FontWeight.Bold)
    )
    val BalooBhai2 = FontFamily(
        Font(R.font.baloo_bhai2, FontWeight.Normal),
        Font(R.font.baloo_bhai2, FontWeight.Bold)
    )
    val NotoSerifDevanagari = FontFamily(Font(R.font.noto_serif_devanagari_regular, FontWeight.Normal))
    val NotoSerifGujarati = FontFamily(Font(R.font.noto_serif_gujarati_regular, FontWeight.Normal))
    val NotoSansDevanagari = FontFamily(Font(R.font.noto_sans_devanagari_regular, FontWeight.Normal))
    val NotoSansGujarati = FontFamily(Font(R.font.noto_sans_gujarati_regular, FontWeight.Normal))

    val displayHero = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    )
    val displayNumber = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 36.sp
    )
    val headline = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    )
    val title = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )
    val body = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
    val bodyReading = TextStyle(
        fontFamily = Literata,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 30.6.sp
    )
    val meta = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    )
    val sticker = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    )
    val displayGiant = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.Black,
        fontSize = 72.sp,
        lineHeight = 74.sp
    )
    val screenTitle = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    )
    val numberMedium = TextStyle(
        fontFamily = Unbounded,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
        lineHeight = 22.sp
    )
    val label = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )
    val labelSmall = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
    val caption = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
    val metaSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
    val quote = TextStyle(
        fontFamily = Literata,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    val quoteSmall = TextStyle(
        fontFamily = Literata,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val readingHeadline = TextStyle(
        fontFamily = Literata,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 38.sp
    )
    val button = TextStyle(
        fontFamily = BricolageGrotesque,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )

    @Suppress("DEPRECATION")
    fun readingFor(languageTag: String, base: TextStyle = bodyReading): TextStyle {
        val (family, multiplier) = when (languageTag.lowercase()) {
            "hi" -> NotoSerifDevanagari to 1.85f
            "gu" -> NotoSerifGujarati to 1.80f
            else -> Literata to 1.70f
        }
        val size = if (base.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified) base.fontSize else 18.sp
        return base.copy(
            fontFamily = family,
            lineHeight = size * multiplier,
            localeList = LocaleList(languageTag),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None
            )
        )
    }

    val materialTypography = Typography(
        displaySmall = displayNumber,
        headlineMedium = displayHero,
        headlineSmall = headline,
        titleLarge = headline,
        titleMedium = title,
        bodyLarge = body,
        bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = body.copy(fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = button,
        labelMedium = sticker,
        labelSmall = meta,
        titleSmall = label,
        displayLarge = displayGiant,
        displayMedium = screenTitle,
        headlineLarge = screenTitle
    )

    /** Base reading size in sp for a reader font-size percentage (100% = 16sp). */
    fun readingSize(percent: Int): androidx.compose.ui.unit.TextUnit = (16f * percent / 100f).sp
}
