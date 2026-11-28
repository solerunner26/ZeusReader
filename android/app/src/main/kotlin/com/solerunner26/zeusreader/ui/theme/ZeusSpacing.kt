package com.solerunner26.zeusreader.ui.theme

import androidx.compose.ui.unit.dp

/** Stormlight spacing scale (4dp grid) + layout constants. Screens never use raw `.dp`. */
object ZeusSpacing {
    val none = 0.dp
    val hairline = 1.dp
    val xxs = 2.dp
    val xs = 4.dp
    val xsPlus = 6.dp
    val sm = 8.dp
    val smPlus = 10.dp
    val md = 12.dp
    val mdPlus = 14.dp
    val lg = 16.dp
    val lgPlus = 18.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val giant = 56.dp

    /** Horizontal screen gutter used by every root screen. */
    val gutter = 20.dp
    /** Vertical rhythm between major sections. */
    val section = 20.dp
    val cardPadding = 18.dp

    val pageInset = 8.dp
    val dockHeight = 68.dp
    val dockInset = 16.dp
    /** dockHeight + 2×dockInset. Add the navigation-bar inset on top (see `rememberDockClearance`). */
    val dockBottomClearance = 100.dp
    val orbSize = 68.dp
    val touchMin = 48.dp
    val primaryButtonHeight = 58.dp
    val secondaryButtonHeight = 48.dp
    val chipHeight = 40.dp
    val topBarHeight = 56.dp
    val glassBlur = 20.dp
    val glassBorderWidth = 1.dp
    val neonBorderWidth = 1.5.dp
    val selectedBorderWidth = 2.dp
}

/** Fixed component sizes (icons, covers, rings, orb, …). */
object ZeusSizes {
    val iconXs = 14.dp
    val iconSm = 16.dp
    val iconMd = 20.dp
    val iconLg = 24.dp
    val iconXl = 32.dp
    val iconHero = 48.dp
    val avatar = 44.dp
    val iconTile = 44.dp
    val iconTileLarge = 56.dp
    val swatch = 52.dp
    val switchWidth = 52.dp
    val switchHeight = 32.dp
    val switchThumb = 24.dp

    val coverThumbWidth = 56.dp
    val coverThumbHeight = 84.dp
    val coverHeroWidth = 150.dp
    val coverHeroHeight = 222.dp
    val coverFlowSideWidth = 104.dp
    val coverFlowSideHeight = 156.dp
    val heroStageHeight = 250.dp
    val coverFlowHeight = 236.dp
    val gridCoverMin = 148.dp

    val chargeRing = 84.dp
    val chargeRingStroke = 7.dp
    val progressRing = 64.dp
    val progressRingStroke = 6.dp
    val progressTrack = 6.dp
    val glowBlob = 220.dp

    val bannerHeight = 36.dp
    val sheetHandleWidth = 40.dp
    val sheetHandleHeight = 4.dp
    val bentoMinHeight = 112.dp
    val quickActionTile = 56.dp

    val orbIcon = 28.dp
    val constellationItem = 56.dp
    val constellationRadius = 132.dp
    val flyRailWidth = 52.dp
    val flyPlanet = 28.dp
    val flyPlanetActive = 56.dp
    val ribbonWidth = 24.dp
    val ribbonHeightIdle = 40.dp
    val ribbonHeightActive = 64.dp
    val foreEdgeVisual = 12.dp
    val foreEdgePadding = 32.dp
    val marginMarkWidth = 4.dp
    val marginMarkHeight = 36.dp
    val focusBandHeight = 84.dp
    val vignetteHeight = 96.dp
    val edgeZoneWidth = 64.dp
    val brightnessZoneWidth = 28.dp
    val waveformHeight = 64.dp
    val waveformBar = 4.dp
    val listenOrb = 88.dp
    val skipButton = 60.dp
    val wordCardMinHeight = 300.dp
    val starMapHeight = 180.dp
    val star = 4.dp
    val starBright = 8.dp
    val sheetMaxListHeight = 420.dp
    val sheetMaxShortList = 300.dp
    val readerMaxWidth = 720.dp
    val contentMaxWidth = 560.dp
    val emptyStateIcon = 72.dp

    val elevationNone = 0.dp
    val shadowSoft = 12.dp
    val shadowCover = 22.dp
    val shadowGlow = 28.dp
    val stickerOffset = 3.dp
}
