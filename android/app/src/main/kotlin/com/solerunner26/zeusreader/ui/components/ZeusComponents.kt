package com.solerunner26.zeusreader.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import com.solerunner26.zeusreader.ui.theme.GlassSurface
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.liquidGlass
import com.solerunner26.zeusreader.ui.theme.neonBorder
import com.solerunner26.zeusreader.ui.theme.rememberHeadingBrush
import com.solerunner26.zeusreader.ui.theme.rememberPrimaryGradient

/* ---------------------------------------------------------------------------------------------
 * Layout helpers
 * ------------------------------------------------------------------------------------------- */

/** Bottom space content must leave so nothing hides behind the floating dock + navigation bar. */
@Composable
fun rememberDockClearance(): Dp =
    ZeusSpacing.dockBottomClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Bottom space for screens without the dock (just the navigation bar + breathing room). */
@Composable
fun rememberNavBarClearance(): Dp =
    ZeusSpacing.xxl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/**
 * Stormlight top bar: optional glass back button, Unbounded title (+ subtitle), trailing actions.
 * Title takes the remaining width and ellipsizes — actions never overlap it.
 */
@Composable
fun ZeusTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    large: Boolean = false,
    animatedTitle: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.topBarHeight)
            .padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        if (onBack != null) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            val style = if (large) type.screenTitle else type.headline
            if (animatedTitle) {
                Text(
                    text = title,
                    style = style.copy(brush = rememberHeadingBrush()),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            } else {
                Text(
                    text = title,
                    style = style,
                    color = colors.textOnSky,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = type.caption,
                    color = colors.textOnSkySecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/* ---------------------------------------------------------------------------------------------
 * Buttons
 * ------------------------------------------------------------------------------------------- */

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = ZeusTheme.colors.textOnSky,
    size: Dp = ZeusSpacing.touchMin,
    highlighted: Boolean = false
) {
    val colors = ZeusTheme.colors
    val base = modifier
        .size(size)
        .then(if (highlighted) Modifier.neonBorder(shape = ZeusShapes.circle, animate = false) else Modifier)
        .liquidGlass(shape = ZeusShapes.circle)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    Box(modifier = base, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else colors.textOnSkyMuted,
            modifier = Modifier.size(ZeusSizes.iconMd)
        )
    }
}

/** Primary action: gradient pill (accent → glow2) with dark sky-base text and a soft glow. */
@Composable
fun PrimaryPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = ZeusSpacing.primaryButtonHeight,
    shimmer: Boolean = true,
    leadingIcon: Boolean = false
) {
    val colors = ZeusTheme.colors
    val sky = ZeusTheme.sky
    val type = ZeusTheme.type
    val brush = rememberPrimaryGradient(animate = shimmer)
    val alpha = if (enabled) 1f else 0.45f
    val glow = if (ZeusTheme.paper.isEink || !enabled) ZeusSizes.elevationNone else ZeusSizes.shadowSoft
    Box(
        modifier = modifier
            .heightIn(min = height)
            .shadow(glow, ZeusShapes.pill, ambientColor = sky.glow2, spotColor = sky.glow2)
            .clip(ZeusShapes.pill)
            .background(brush, alpha = alpha)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = ZeusSpacing.xl, vertical = ZeusSpacing.sm),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
        ) {
            val fg = colors.textOnAccent
            if (icon != null && leadingIcon) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(ZeusSizes.iconMd))
            }
            Text(
                text = label,
                style = type.button,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (icon != null && !leadingIcon) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(ZeusSizes.iconMd))
            }
        }
    }
}

/** Secondary action: glass pill with sky text. [danger] tints it with the error glow. */
@Composable
fun GlassPillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    height: Dp = ZeusSpacing.secondaryButtonHeight
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val fg = when {
        !enabled -> colors.textOnSkyMuted
        danger -> colors.errorGlow
        else -> colors.textOnSky
    }
    Row(
        modifier = modifier
            .heightIn(min = height)
            .liquidGlass(shape = ZeusShapes.pill)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = ZeusSpacing.lg, vertical = ZeusSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm, Alignment.CenterHorizontally)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (danger) fg else colors.accentOnSky, modifier = Modifier.size(ZeusSizes.iconSm))
        }
        Text(
            text = label,
            style = type.label,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Quick action tile (icon tile + label) — like a banking app's action row. */
@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(
        modifier = modifier
            .clip(ZeusShapes.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = ZeusSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
    ) {
        IconTile(icon = icon, size = ZeusSizes.quickActionTile, filled = accent)
        Text(
            text = label,
            style = type.labelSmall,
            color = colors.textOnSkySecondary,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Rounded icon tile used in lists, quick actions and settings rows. */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = ZeusSizes.iconTile,
    filled: Boolean = false,
    tint: Color? = null
) {
    val colors = ZeusTheme.colors
    val bg = if (filled) {
        Modifier.background(rememberPrimaryGradient(animate = false), ZeusShapes.tile)
    } else {
        Modifier.liquidGlass(shape = ZeusShapes.tile)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(ZeusShapes.tile)
            .then(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: if (filled) colors.textOnAccent else colors.accentOnSky,
            modifier = Modifier.size(ZeusSizes.iconLg)
        )
    }
}

/* ---------------------------------------------------------------------------------------------
 * Chips, segmented control, stickers
 * ------------------------------------------------------------------------------------------- */

@Composable
fun ZeusChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    labelStyle: TextStyle = ZeusTheme.type.label
) {
    val colors = ZeusTheme.colors
    val shape = ZeusShapes.chip
    val fg = when {
        !enabled -> colors.textOnSkyMuted
        selected -> colors.chipSelectedFg
        else -> colors.textOnSky
    }
    val surface = if (selected) {
        Modifier
            .clip(shape)
            .background(colors.chipSelectedBg, shape)
    } else {
        Modifier.liquidGlass(shape = shape)
    }
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = ZeusSpacing.chipHeight)
            .then(surface)
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = ZeusSpacing.mdPlus, vertical = ZeusSpacing.xsPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(ZeusSizes.iconSm))
        }
        Text(
            text = label,
            style = labelStyle,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Horizontally scrolling chip row that bleeds to the screen edges but pads its content. */
@Composable
fun ZeusChipRow(
    modifier: Modifier = Modifier,
    contentPadding: Dp = ZeusSpacing.gutter,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Pill segmented control — selected segment is a solid starlight/ink capsule. */
@Composable
fun ZeusSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = ZeusShapes.pill)
            .padding(ZeusSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = ZeusSpacing.touchMin - ZeusSpacing.xs)
                    .clip(ZeusShapes.pill)
                    .then(if (selected) Modifier.background(colors.chipSelectedBg) else Modifier)
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .semantics { this.selected = selected }
                    .padding(horizontal = ZeusSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = type.label,
                    color = if (selected) colors.chipSelectedFg else colors.textOnSkySecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Tilted sticker chip (DESIGN.md §3): light fill, near-black text, offset glow shadow. */
@Composable
fun StickerChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    rotation: Float = 3f,
    cream: Boolean = false,
    accentFill: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = ZeusTheme.colors
    val sky = ZeusTheme.sky
    val type = ZeusTheme.type
    val bg = when {
        accentFill -> sky.accent
        cream -> colors.stickerCreamBg
        else -> colors.stickerLightBg
    }
    val angle = if (ZeusTheme.paper.isEink) 0f else rotation
    val touch = if (onClick != null) {
        Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Box(modifier = modifier.then(touch), contentAlignment = Alignment.Center) {
    Box(modifier = Modifier.rotate(angle)) {
        if (!ZeusTheme.paper.isEink) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = ZeusSizes.stickerOffset, top = ZeusSizes.stickerOffset)
                    .background(colors.neonA, ZeusShapes.sticker)
            )
        }
        Row(
            modifier = Modifier
                .padding(end = ZeusSizes.stickerOffset, bottom = ZeusSizes.stickerOffset)
                .heightIn(min = ZeusSpacing.huge - ZeusSpacing.xs)
                .clip(ZeusShapes.sticker)
                .background(bg)
                .then(if (ZeusTheme.paper.isEink) Modifier.border(ZeusSpacing.hairline, colors.textOnSky, ZeusShapes.sticker) else Modifier)
                .padding(horizontal = ZeusSpacing.md, vertical = ZeusSpacing.xsPlus),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.stickerInk, modifier = Modifier.size(ZeusSizes.iconSm))
            }
            Text(text = text, style = type.sticker, color = colors.stickerInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Section header, rows, toggles
 * ------------------------------------------------------------------------------------------- */

@Composable
fun ZeusSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.touchMin - ZeusSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
    ) {
        Text(
            text = title,
            style = type.title,
            color = colors.textOnSky,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = type.label,
                color = colors.accentOnSky,
                maxLines = 1,
                modifier = Modifier
                    .clip(ZeusShapes.pill)
                    .clickable(role = Role.Button, onClick = onAction)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = ZeusSpacing.sm)
            )
        }
    }
}

/** Small mono eyebrow label (CH 4 · WATER IN SOIL). */
@Composable
fun MetaLabel(text: String, modifier: Modifier = Modifier, accent: Boolean = false, maxLines: Int = 1) {
    val colors = ZeusTheme.colors
    Text(
        text = text.uppercase(),
        style = ZeusTheme.type.meta,
        color = if (accent) colors.accentOnSky else colors.textOnSkySecondary,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** List row: icon tile + title/subtitle (weighted, never overlaps) + trailing slot. */
@Composable
fun ZeusListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconFilled: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    titleMaxLines: Int = 2,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.touchMin + ZeusSpacing.sm)
            .then(if (onClick != null) Modifier.clip(ZeusShapes.tile).clickable(onClick = onClick) else Modifier)
            .padding(vertical = ZeusSpacing.xsPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        when {
            leading != null -> leading()
            icon != null -> IconTile(icon = icon, filled = iconFilled)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs)) {
            Text(text = title, style = type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.textOnSky, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, style = type.caption, color = colors.textOnSkySecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

@Composable
fun ZeusSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = ZeusTheme.colors
    val sky = ZeusTheme.sky
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = if (colors.isLight) sky.lightA else sky.baseA,
            checkedTrackColor = if (colors.isLight) sky.accentInk else sky.accent,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = colors.textOnSkySecondary,
            uncheckedTrackColor = colors.trackInactive,
            uncheckedBorderColor = colors.glassBorder
        )
    )
}


@Composable
fun ZeusSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.touchMin + ZeusSpacing.xs)
            .clip(ZeusShapes.tile)
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .padding(vertical = ZeusSpacing.xsPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = colors.accentOnSky, modifier = Modifier.size(ZeusSizes.iconMd))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs)) {
            Text(text = title, style = type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.textOnSky, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(text = subtitle, style = type.caption, color = colors.textOnSkySecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        ZeusSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun ZeusDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(ZeusSpacing.hairline)
            .background(ZeusTheme.colors.divider)
    )
}

/** Glass card section with consistent padding and an optional title. */
@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    neon: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = ZeusSpacing.cardPadding, vertical = ZeusSpacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), shape = ZeusShapes.card, neon = neon) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)
        ) {
            if (title != null) {
                MetaLabel(text = title, accent = true)
            }
            content()
        }
    }
}

/* ---------------------------------------------------------------------------------------------
 * Inputs
 * ------------------------------------------------------------------------------------------- */

@Composable
fun zeusTextFieldColors() = run {
    val colors = ZeusTheme.colors
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textOnSky,
        unfocusedTextColor = colors.textOnSky,
        disabledTextColor = colors.textOnSkyMuted,
        cursorColor = colors.accentOnSky,
        focusedBorderColor = colors.accentOnSky,
        unfocusedBorderColor = colors.glassBorder,
        focusedLabelColor = colors.accentOnSky,
        unfocusedLabelColor = colors.textOnSkySecondary,
        focusedPlaceholderColor = colors.textOnSkyMuted,
        unfocusedPlaceholderColor = colors.textOnSkyMuted,
        focusedLeadingIconColor = colors.textOnSkySecondary,
        unfocusedLeadingIconColor = colors.textOnSkySecondary,
        focusedTrailingIconColor = colors.textOnSkySecondary,
        unfocusedTrailingIconColor = colors.textOnSkySecondary,
        focusedContainerColor = colors.glassFillSubtle,
        unfocusedContainerColor = colors.glassFillSubtle
    )
}

@Composable
fun ZeusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true
) {
    val type = ZeusTheme.type
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        textStyle = type.body,
        label = label?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        placeholder = placeholder?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, modifier = Modifier.size(ZeusSizes.iconMd)) } },
        trailingIcon = trailing,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = ZeusShapes.field,
        colors = zeusTextFieldColors()
    )
}

@Composable
fun ZeusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val colors = ZeusTheme.colors
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = colors.accentOnSky,
            activeTrackColor = colors.accentOnSky,
            inactiveTrackColor = colors.trackInactive,
            activeTickColor = colors.textOnAccent,
            inactiveTickColor = colors.textOnSkyMuted
        )
    )
}

/** Labelled slider row: title on the left, value on the right, slider below. */
@Composable
fun ZeusSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = type.label.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = colors.textOnSky, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(valueLabel, style = type.meta, color = colors.accentOnSky, maxLines = 1)
        }
        ZeusSlider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

@Composable
fun ZeusLinearProgress(progress: Float? = null, modifier: Modifier = Modifier) {
    val colors = ZeusTheme.colors
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(ZeusSizes.progressTrack)
                .clip(ZeusShapes.pill),
            color = colors.accentOnSky,
            trackColor = colors.trackInactive
        )
    } else {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier
                .fillMaxWidth()
                .height(ZeusSizes.progressTrack)
                .clip(ZeusShapes.pill),
            color = colors.accentOnSky,
            trackColor = colors.trackInactive,
            drawStopIndicator = {}
        )
    }
}

/** Circular progress ring (charge ring / donut) with centred content. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = ZeusSizes.progressRing,
    stroke: Dp = ZeusSizes.progressRingStroke,
    animate: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val colors = ZeusTheme.colors
    val sky = ZeusTheme.sky
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = com.solerunner26.zeusreader.ui.theme.ZeusMotion.settleTween(ZeusTheme.reduceMotion || !animate, 700),
        label = "progress_ring"
    )
    val arcBrush = if (ZeusTheme.paper.isEink) {
        Brush.linearGradient(listOf(colors.textOnSky, colors.textOnSky))
    } else {
        Brush.sweepGradient(listOf(sky.accent, sky.glow2, sky.accent))
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val inset = s / 2f
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - s, this.size.height - s)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(color = colors.trackInactive, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(s, cap = StrokeCap.Round))
            drawArc(brush = arcBrush, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

/* ---------------------------------------------------------------------------------------------
 * Sheets, dialogs, empty states, snackbars
 * ------------------------------------------------------------------------------------------- */

/**
 * Stormlight bottom sheet: 34dp top corners, sky-base fill, glass handle, scrollable body,
 * navigation-bar safe. [title] renders as an Unbounded headline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeusBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    skipPartiallyExpanded: Boolean = true,
    scrollable: Boolean = true,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = ZeusShapes.sheet,
        containerColor = colors.sheetBackground,
        contentColor = colors.textOnSky,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = ZeusSpacing.md, bottom = ZeusSpacing.xs)
                    .size(width = ZeusSizes.sheetHandleWidth, height = ZeusSizes.sheetHandleHeight)
                    .clip(ZeusShapes.pill)
                    .background(colors.textOnSkyMuted)
            )
        },
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.textOnSky) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = ZeusSpacing.gutter)
                    .padding(bottom = ZeusSpacing.xxl)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
            ) {
                if (title != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = type.headline, color = colors.textOnSky, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                            if (!subtitle.isNullOrBlank()) {
                                Text(subtitle, style = type.caption, color = colors.textOnSkySecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        headerAction?.invoke(this)
                    }
                }
                content()
            }
        }
    }
}

/** Themed alert dialog with gradient confirm and glass dismiss. */
@Composable
fun ZeusDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String? = "Cancel",
    onDismiss: () -> Unit = onDismissRequest,
    confirmEnabled: Boolean = true,
    dangerConfirm: Boolean = false,
    body: @Composable ColumnScope.() -> Unit = {}
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = ZeusShapes.dialog,
        containerColor = colors.sheetBackground,
        titleContentColor = colors.textOnSky,
        textContentColor = colors.textOnSky,
        title = { Text(title, style = type.headline, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), content = body)
        },
        confirmButton = {
            if (dangerConfirm) {
                GlassPillButton(label = confirmLabel, onClick = onConfirm, enabled = confirmEnabled, danger = true)
            } else {
                PrimaryPillButton(label = confirmLabel, onClick = onConfirm, enabled = confirmEnabled, height = ZeusSpacing.secondaryButtonHeight, shimmer = false)
            }
        },
        dismissButton = dismissLabel?.let {
            { GlassPillButton(label = it, onClick = onDismiss) }
        }
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    actionIcon: ImageVector? = null
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    GlassSurface(modifier = modifier.widthIn(max = ZeusSizes.contentMaxWidth).fillMaxWidth(), neon = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ZeusSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(ZeusSizes.emptyStateIcon)
                    .clip(ZeusShapes.circle)
                    .background(rememberPrimaryGradient(animate = false)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = colors.textOnAccent, modifier = Modifier.size(ZeusSizes.iconXl))
            }
            Text(title, style = type.headline, color = colors.textOnSky, textAlign = TextAlign.Center)
            Text(message, style = type.body, color = colors.textOnSkySecondary, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(ZeusSpacing.xs))
                PrimaryPillButton(label = actionLabel, onClick = onAction, icon = actionIcon, leadingIcon = true, modifier = Modifier.fillMaxWidth())
            }
            if (secondaryLabel != null && onSecondary != null) {
                GlassPillButton(label = secondaryLabel, onClick = onSecondary, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun ZeusSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = ZeusTheme.colors
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = ZeusShapes.cardSmall,
            containerColor = colors.chipSelectedBg,
            contentColor = colors.chipSelectedFg,
            actionColor = if (colors.isLight) ZeusTheme.sky.accent else ZeusTheme.sky.accentInk
        )
    }
}

/** Gradient-animated heading text (two-tone shimmer). */
@Composable
fun GradientHeading(text: String, modifier: Modifier = Modifier, style: TextStyle = ZeusTheme.type.screenTitle, maxLines: Int = 1) {
    Text(
        text = text,
        style = style.copy(brush = rememberHeadingBrush()),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { heading() }
    )
}

/** Value + label stat tile. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    caption: String? = null,
    onClick: (() -> Unit)? = null
) {
    val colors = ZeusTheme.colors
    val type = ZeusTheme.type
    GlassSurface(modifier = modifier, shape = ZeusShapes.cardCompact) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(ZeusSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.accentOnSky, modifier = Modifier.size(ZeusSizes.iconLg))
            }
            Text(value, style = type.displayNumber, color = colors.textOnSky, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = type.label, color = colors.textOnSky, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!caption.isNullOrBlank()) {
                Text(caption, style = type.caption, color = colors.textOnSkySecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A thin gradient accent bar (used under headings / active states). */
@Composable
fun AccentBar(modifier: Modifier = Modifier, width: Dp = ZeusSpacing.xxxl) {
    Box(
        modifier = modifier
            .width(width)
            .height(ZeusSpacing.xs)
            .clip(ZeusShapes.pill)
            .background(rememberPrimaryGradient(animate = false))
    )
}

