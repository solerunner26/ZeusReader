package com.solerunner26.zeusreader.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.solerunner26.zeusreader.ui.theme.ZeusMotion
import com.solerunner26.zeusreader.ui.theme.ZeusShapes
import com.solerunner26.zeusreader.ui.theme.ZeusSizes
import com.solerunner26.zeusreader.ui.theme.ZeusSpacing
import com.solerunner26.zeusreader.ui.theme.ZeusTheme
import com.solerunner26.zeusreader.ui.theme.neonBorder
import com.solerunner26.zeusreader.ui.theme.pressScale
import com.solerunner26.zeusreader.ui.theme.rememberAccentBrush
import com.solerunner26.zeusreader.ui.theme.zeusCard

/* ============================================================================================
 * Layout helpers
 * ========================================================================================== */

/** Bottom space so content never hides behind the bottom navigation bar (+ system nav bar). */
@Composable
fun rememberNavClearance(): Dp =
    ZeusSpacing.navClearance + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Bottom space for screens without the bottom navigation (system nav bar + breathing room). */
@Composable
fun rememberNavBarClearance(): Dp =
    ZeusSpacing.xxl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

/** Caps font scaling for fixed-size artwork (covers, rings, nav) so glyphs never split or clip. */
@Composable
fun CappedFontScale(max: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(max)), content = content)
}

/** Staggered entrance: rises from 10dp and fades from 35% on first composition (never starts invisible). */
@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val reduce = ZeusTheme.reduceMotion
    val anim = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            kotlinx.coroutines.delay(index.coerceAtMost(8) * ZeusMotion.STAGGER_MS.toLong())
            anim.animateTo(1f, ZeusMotion.fill(false, 600))
        }
    }
    return this.graphicsLayer {
        val v = anim.value
        alpha = 0.35f + 0.65f * v.coerceIn(0f, 1f)
        translationY = (1f - v) * 10f * density
    }
}

/* ============================================================================================
 * Text
 * ========================================================================================== */

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, accent: Boolean = true, color: Color? = null, maxLines: Int = 1) {
    val c = ZeusTheme.colors
    Text(
        text = text.uppercase(),
        style = ZeusTheme.type.eyebrow,
        color = color ?: if (accent) c.accentInk else c.muted,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** Large screen title + optional muted subtitle. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    val c = ZeusTheme.colors
    Column(modifier = modifier) {
        Text(title, style = ZeusTheme.type.title, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, style = ZeusTheme.type.caption, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Top bar: round back button, title (takes remaining width, ellipsizes) and actions. */
@Composable
fun ZeusTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.topBarHeight)
            .padding(horizontal = ZeusSpacing.gutter, vertical = ZeusSpacing.xsPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        if (onBack != null) CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        ScreenTitle(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/* ============================================================================================
 * Buttons
 * ========================================================================================== */

/** 44–48dp round card button with soft shadow; optional accent badge dot (e.g. unread). */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: Boolean = false,
    filled: Boolean = false,
    tint: Color? = null,
    size: Dp = ZeusSpacing.touchMin
) {
    val c = ZeusTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(source)
            .then(
                if (filled) {
                    Modifier
                        .shadow(ZeusSizes.shadowCard, ZeusShapes.circle, ambientColor = c.glow, spotColor = c.glow)
                        .clip(ZeusShapes.circle)
                        .background(rememberAccentBrush())
                } else {
                    Modifier.zeusCard(ZeusShapes.circle)
                }
            )
            .clickable(source, indication = androidx.compose.material3.ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: when {
                !enabled -> c.muted.copy(alpha = 0.5f)
                filled -> c.onAccent
                else -> c.ink
            },
            modifier = Modifier.size(ZeusSizes.iconMd)
        )
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -ZeusSpacing.smPlus, y = ZeusSpacing.smPlus)
                    .size(ZeusSpacing.smPlus)
                    .clip(ZeusShapes.circle)
                    .background(c.card)
                    .padding(ZeusSpacing.xxs)
                    .clip(ZeusShapes.circle)
                    .background(c.accent)
            )
        }
    }
}

/** Primary CTA: full-pill accent gradient with a slow shine, glow shadow and spring press. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingIcon: Boolean = false,
    enabled: Boolean = true,
    height: Dp = ZeusSpacing.primaryButtonHeight,
    shine: Boolean = true
) {
    val c = ZeusTheme.colors
    val source = remember { MutableInteractionSource() }
    val brush = rememberAccentBrush(shine = shine)
    Row(
        modifier = modifier
            .heightIn(min = height)
            .pressScale(source)
            .then(if (enabled && !c.isEink) Modifier.shadow(ZeusSizes.shadowCard, ZeusShapes.pill, ambientColor = c.glow, spotColor = c.glow) else Modifier)
            .clip(ZeusShapes.pill)
            .background(brush, alpha = if (enabled) 1f else 0.45f)
            .clickable(source, indication = androidx.compose.material3.ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = ZeusSpacing.xl, vertical = ZeusSpacing.sm),
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null && leadingIcon) Icon(icon, null, tint = c.onAccent, modifier = Modifier.size(ZeusSizes.iconMd))
        Text(label, style = ZeusTheme.type.button, color = c.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (icon != null && !leadingIcon) Icon(icon, null, tint = c.onAccent, modifier = Modifier.size(ZeusSizes.iconMd))
    }
}

/** Secondary CTA: white/charcoal card pill. [danger] tints the label rose. */
@Composable
fun SecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    height: Dp = ZeusSpacing.secondaryButtonHeight
) {
    val c = ZeusTheme.colors
    val source = remember { MutableInteractionSource() }
    val fg = when {
        !enabled -> c.muted
        danger -> c.rose
        else -> c.ink
    }
    Row(
        modifier = modifier
            .heightIn(min = height)
            .pressScale(source)
            .zeusCard(ZeusShapes.pill)
            .clickable(source, indication = androidx.compose.material3.ripple(), enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = ZeusSpacing.lg, vertical = ZeusSpacing.xsPlus),
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, null, tint = if (danger) fg else c.accentInk, modifier = Modifier.size(ZeusSizes.iconSm))
        Text(label, style = ZeusTheme.type.label, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Small white pill used on top of gradient hero cards ("Resume"). */
@Composable
fun PillButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = ZeusTheme.colors
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .heightIn(min = ZeusSpacing.pillButtonHeight)
            .minimumInteractiveComponentSize()
            .pressScale(source)
            .shadow(ZeusSizes.shadowCard, ZeusShapes.pill)
            .clip(ZeusShapes.pill)
            .background(c.card)
            .clickable(source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick)
            .padding(horizontal = ZeusSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = ZeusTheme.type.label, color = c.ink, maxLines = 1)
        if (icon != null) Icon(icon, null, tint = c.ink, modifier = Modifier.size(ZeusSizes.iconSm))
    }
}

/** Plain accent text action ("See all", "Reset"). */
@Composable
fun TextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = ZeusTheme.type.label,
        color = ZeusTheme.colors.accentInk,
        maxLines = 1,
        modifier = modifier
            .clip(ZeusShapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = ZeusSpacing.xs)
    )
}

/* ============================================================================================
 * Icon boxes, chips, segmented, switches, rows
 * ========================================================================================== */

enum class Tone { Accent, Info, Ok, Rose, Neutral }

@Composable
fun IconBox(icon: ImageVector, modifier: Modifier = Modifier, tone: Tone = Tone.Accent, size: Dp = ZeusSizes.iconBox, filled: Boolean = false, circle: Boolean = false) {
    val c = ZeusTheme.colors
    val (bg, fg) = when (tone) {
        Tone.Accent -> c.tint to c.accentInk
        Tone.Info -> c.infoTint to c.info
        Tone.Ok -> c.okTint to c.ok
        Tone.Rose -> c.roseTint to c.rose
        Tone.Neutral -> c.canvas to c.ink
    }
    val shape = if (circle) ZeusShapes.circle else ZeusShapes.iconBox
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(if (filled) Modifier.background(rememberAccentBrush()) else Modifier.background(bg)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (filled) c.onAccent else fg, modifier = Modifier.size(ZeusSizes.iconMd))
    }
}

/** Pill chip: tint when idle, accent gradient when selected; [outline] for neutral filter chips. */
@Composable
fun ZeusChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    outline: Boolean = false,
    labelStyle: TextStyle = ZeusTheme.type.label
) {
    val c = ZeusTheme.colors
    val fg = when {
        !enabled -> c.muted
        selected -> c.onAccent
        outline -> c.ink
        else -> c.accentInk
    }
    val surface = when {
        selected -> Modifier.background(rememberAccentBrush())
        outline -> Modifier
            .background(c.card)
            .border(ZeusSpacing.borderWidth, c.line, ZeusShapes.pill)
        else -> Modifier.background(c.tint)
    }
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = ZeusSpacing.chipHeight)
            .clip(ZeusShapes.pill)
            .then(surface)
            .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = ZeusSpacing.mdPlus, vertical = ZeusSpacing.xsPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus)
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(ZeusSizes.iconSm))
        Text(label, style = labelStyle, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ZeusChipRow(modifier: Modifier = Modifier, contentPadding: Dp = ZeusSpacing.gutter, content: @Composable RowScope.() -> Unit) {
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

/** Segmented control with a sliding gradient knob (Day/Night style). */
@Composable
fun ZeusSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?>? = null
) {
    val c = ZeusTheme.colors
    val count = options.size.coerceAtLeast(1)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.touchMin + ZeusSpacing.xxs)
            .clip(ZeusShapes.pill)
            .background(c.canvas)
            .border(ZeusSpacing.borderWidth, c.line, ZeusShapes.pill)
            .padding(ZeusSpacing.xs)
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.matchParentSize()) {
            val segment = maxWidth / count
            val x by animateDpAsState(segment * selectedIndex.coerceIn(0, count - 1), ZeusMotion.bouncy(ZeusTheme.reduceMotion), label = "seg_knob")
            Box(
                modifier = Modifier
                    .offset(x = x)
                    .width(segment)
                    .fillMaxHeight()
                    .shadow(ZeusSizes.shadowCard, ZeusShapes.pill, ambientColor = c.glow, spotColor = c.glow)
                    .clip(ZeusShapes.pill)
                    .background(rememberAccentBrush())
            )
        }
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                val sel = i == selectedIndex
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = ZeusSpacing.touchMin - ZeusSpacing.xsPlus)
                        .clip(ZeusShapes.pill)
                        .clickable(role = Role.Tab) { onSelect(i) }
                        .semantics { this.selected = sel }
                        .padding(horizontal = ZeusSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xsPlus, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icons?.getOrNull(i)?.let { Icon(it, null, tint = if (sel) c.onAccent else c.muted, modifier = Modifier.size(ZeusSizes.iconSm)) }
                    Text(option, style = ZeusTheme.type.label, color = if (sel) c.onAccent else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Animated pill toggle: gradient track when on, springing white knob. */
@Composable
fun ZeusSwitch(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = ZeusTheme.colors
    val knobX by animateDpAsState(if (checked) ZeusSpacing.xl + ZeusSpacing.xxs + ZeusSpacing.hairline else ZeusSpacing.none, ZeusMotion.bouncy(ZeusTheme.reduceMotion), label = "switch_knob")
    Box(
        modifier = modifier
            .size(width = ZeusSizes.iconXl + ZeusSpacing.lg + ZeusSpacing.xxs, height = ZeusSpacing.xxxl - ZeusSpacing.xxs)
            .clip(ZeusShapes.pill)
            .then(if (checked) Modifier.background(rememberAccentBrush()) else Modifier.background(c.line))
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .padding(ZeusSpacing.xxs + ZeusSpacing.hairline)
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobX)
                .size(ZeusSizes.iconLg)
                .shadow(ZeusSpacing.xxs, ZeusShapes.circle)
                .clip(ZeusShapes.circle)
                .background(if (c.isEink && checked) c.canvas else com.solerunner26.zeusreader.ui.theme.ZeusPalette.white)
        )
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val c = ZeusTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.giant + ZeusSpacing.xxs)
            .clip(ZeusShapes.tile)
            .clickable(enabled = enabled, role = Role.Switch) { onCheckedChange(!checked) }
            .semantics { stateDescription = if (checked) "On" else "Off" }
            .padding(vertical = ZeusSpacing.xsPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        if (icon != null) IconBox(icon, size = ZeusSizes.iconBoxSmall)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs)) {
            Text(title, style = ZeusTheme.type.label, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = ZeusTheme.type.caption, color = c.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        ZeusSwitch(checked = checked, enabled = enabled)
    }
}

/** Menu / list row: icon box · title + subtitle (weighted, never overlaps) · trailing. */
@Composable
fun MenuRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tone: Tone = Tone.Accent,
    value: String? = null,
    chevron: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    titleMaxLines: Int = 2,
    titleStyle: TextStyle = ZeusTheme.type.label,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val c = ZeusTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ZeusSpacing.giant)
            .then(if (onClick != null) Modifier.clip(ZeusShapes.tile).clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(vertical = ZeusSpacing.xsPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
    ) {
        when {
            leading != null -> leading()
            icon != null -> IconBox(icon, tone = tone)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ZeusSpacing.xxs)) {
            Text(title, style = titleStyle, color = c.ink, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) Text(subtitle, style = ZeusTheme.type.caption, color = c.muted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) Text(value, style = ZeusTheme.type.label, color = c.muted, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        if (chevron) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted, modifier = Modifier.size(ZeusSizes.iconMd))
    }
}

@Composable
fun ZeusDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(ZeusSpacing.hairline)
            .background(ZeusTheme.colors.line)
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = ZeusTheme.type.headline, color = ZeusTheme.colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { heading() })
        if (actionLabel != null && onAction != null) TextAction(actionLabel, onAction)
    }
}

/** Elevated content card (20dp radius, soft shadow). */
@Composable
fun ZeusCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(ZeusSpacing.cardPadding),
    animateSize: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val c = ZeusTheme.colors
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(source) else Modifier)
            .neonBorder(selected, ZeusShapes.card)
            .zeusCard(ZeusShapes.card)
            .then(if (onClick != null) Modifier.clickable(source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick) else Modifier)
            .then(if (animateSize && !ZeusTheme.reduceMotion) Modifier.animateContentSize() else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(ZeusSpacing.smPlus)
    ) {
        CompositionLocalProvider(LocalContentColor provides c.ink) { content() }
    }
}

/** Gradient hero card (the one big accent surface per screen). */
@Composable
fun GradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(ZeusSpacing.gutter),
    starburst: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val c = ZeusTheme.colors
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(source) else Modifier)
            .then(if (!c.isEink) Modifier.shadow(ZeusSizes.shadowLift, ZeusShapes.cardLarge, ambientColor = c.glow, spotColor = c.glow) else Modifier)
            .clip(ZeusShapes.cardLarge)
            .background(rememberAccentBrush(shine = true))
            .then(if (c.isEink) Modifier.border(ZeusSpacing.borderWidth, c.ink, ZeusShapes.cardLarge) else Modifier)
            .then(if (onClick != null) Modifier.clickable(source, indication = androidx.compose.material3.ripple(), role = Role.Button, onClick = onClick) else Modifier)
    ) {
        if (starburst) {
            Starburst(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = ZeusSizes.heroMascot / 2, y = -ZeusSizes.iconXl)
                    .size(ZeusSizes.stage)
            )
        }
        CompositionLocalProvider(LocalContentColor provides c.onAccent) {
            Box(Modifier.padding(padding), content = content)
        }
    }
}

/** Icon box + big number + label metric tile. */
@Composable
fun MetricCard(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier, tone: Tone = Tone.Accent, onClick: (() -> Unit)? = null, art: (@Composable () -> Unit)? = null) {
    ZeusCard(modifier = modifier, onClick = onClick) {
        if (art != null) art() else IconBox(icon, tone = tone)
        Text(value, style = ZeusTheme.type.number, color = ZeusTheme.colors.ink, maxLines = 1)
        Text(label, style = ZeusTheme.type.caption, color = ZeusTheme.colors.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/* ============================================================================================
 * Progress
 * ========================================================================================== */

/** Rounded progress bar that fills on load with the accent gradient. */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = ZeusSizes.progressTrack, onAccent: Boolean = false) {
    val c = ZeusTheme.colors
    val anim = remember { Animatable(0f) }
    val reduce = ZeusTheme.reduceMotion
    LaunchedEffect(progress) {
        if (reduce) anim.snapTo(progress.coerceIn(0f, 1f)) else anim.animateTo(progress.coerceIn(0f, 1f), ZeusMotion.fill(false))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(ZeusShapes.bar)
            .background(if (onAccent) c.onAccent.copy(alpha = 0.22f) else c.tint)
            .semantics { stateDescription = "${(progress * 100).toInt()} percent" }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(anim.value.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(ZeusShapes.bar)
                .then(if (onAccent) Modifier.background(c.card) else Modifier.background(rememberAccentBrush()))
        )
    }
}

/** Thick circular progress ring that fills on load; content is centred. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = ZeusSizes.ring,
    stroke: Dp = ZeusSizes.ringStroke,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val c = ZeusTheme.colors
    val reduce = ZeusTheme.reduceMotion
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        if (reduce) anim.snapTo(progress.coerceIn(0f, 1f)) else anim.animateTo(progress.coerceIn(0f, 1f), ZeusMotion.fill(false, 1600))
    }
    Box(modifier.size(size).semantics { stateDescription = "${(progress * 100).toInt()} percent" }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val tl = Offset(s / 2f, s / 2f)
            val sz = Size(this.size.width - s, this.size.height - s)
            drawArc(c.tint, -90f, 360f, false, tl, sz, style = Stroke(s))
            drawArc(
                brush = if (c.gradientEnabled) Brush.sweepGradient(listOf(c.accent, c.accent2, c.accent)) else Brush.linearGradient(listOf(c.accent, c.accent)),
                startAngle = -90f, sweepAngle = 360f * anim.value, useCenter = false, topLeft = tl, size = sz,
                style = Stroke(s, cap = StrokeCap.Round)
            )
        }
        CappedFontScale(1.2f) { Box(contentAlignment = Alignment.Center, content = content) }
    }
}

/** Carousel pagination dots that morph into a pill for the current page. */
@Composable
fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier, onSelect: ((Int) -> Unit)? = null) {
    val c = ZeusTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(if (onSelect != null) ZeusSpacing.none else ZeusSpacing.xsPlus), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val on = i == current
            val w by animateDpAsState(if (on) ZeusSpacing.xxl + ZeusSpacing.xxs else ZeusSpacing.sm, ZeusMotion.bouncy(ZeusTheme.reduceMotion), label = "dot_w")
            Box(
                modifier = Modifier
                    .then(
                        if (onSelect != null) {
                            Modifier
                                .clip(ZeusShapes.pill)
                                .clickable(role = Role.Tab) { onSelect(i) }
                                .semantics { contentDescription = "Page ${i + 1} of $count" }
                                .heightIn(min = ZeusSpacing.huge)
                                .widthIn(min = ZeusSpacing.huge)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .height(ZeusSpacing.sm)
                        .width(w)
                        .clip(ZeusShapes.pill)
                        .then(if (on) Modifier.background(rememberAccentBrush()) else Modifier.background(c.line))
                )
            }
        }
    }
}

/* ============================================================================================
 * Inputs
 * ========================================================================================== */

@Composable
fun zeusTextFieldColors() = run {
    val c = ZeusTheme.colors
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.ink, unfocusedTextColor = c.ink, disabledTextColor = c.muted,
        cursorColor = c.accentInk,
        focusedBorderColor = c.accent, unfocusedBorderColor = c.line,
        focusedLabelColor = c.accentInk, unfocusedLabelColor = c.muted,
        focusedPlaceholderColor = c.muted, unfocusedPlaceholderColor = c.muted,
        focusedLeadingIconColor = c.muted, unfocusedLeadingIconColor = c.muted,
        focusedTrailingIconColor = c.muted, unfocusedTrailingIconColor = c.muted,
        focusedContainerColor = c.card, unfocusedContainerColor = c.card
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
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        textStyle = ZeusTheme.type.body,
        label = label?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        placeholder = placeholder?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        leadingIcon = leadingIcon?.let { { Icon(it, null, modifier = Modifier.size(ZeusSizes.iconMd)) } },
        trailingIcon = trailing,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = ZeusShapes.input,
        colors = zeusTextFieldColors()
    )
}

@Composable
fun ZeusSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f, steps: Int = 0) {
    val c = ZeusTheme.colors
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.tint,
            activeTickColor = c.onAccent, inactiveTickColor = c.muted
        )
    )
}

@Composable
fun SliderRow(title: String, valueLabel: String, value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f, steps: Int = 0) {
    val c = ZeusTheme.colors
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = ZeusTheme.type.label, color = c.ink, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(valueLabel, style = ZeusTheme.type.label, color = c.accentInk, maxLines = 1)
        }
        ZeusSlider(value, onValueChange, valueRange = valueRange, steps = steps)
    }
}

/* ============================================================================================
 * Sheets, dialogs, empty states, snackbars
 * ========================================================================================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZeusBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    scrollable: Boolean = true,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = ZeusTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = ZeusShapes.sheet,
        containerColor = c.card,
        contentColor = c.ink,
        scrimColor = c.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = ZeusSpacing.smPlus, bottom = ZeusSpacing.xs)
                    .size(ZeusSizes.sheetHandleWidth, ZeusSizes.sheetHandleHeight)
                    .clip(ZeusShapes.pill)
                    .background(c.line)
            )
        },
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalContentColor provides c.ink) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = ZeusSpacing.xl)
                    .padding(bottom = ZeusSpacing.xxl)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)
            ) {
                if (title != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.sm)) {
                        Column(Modifier.weight(1f)) {
                            Text(title, style = ZeusTheme.type.headline.copy(fontSize = ZeusTheme.type.title.fontSize.times(0.82f)), color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() })
                            if (!subtitle.isNullOrBlank()) Text(subtitle, style = ZeusTheme.type.caption, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        headerAction?.invoke(this)
                    }
                }
                content()
            }
        }
    }
}

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
    val c = ZeusTheme.colors
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = ZeusShapes.dialog,
        containerColor = c.card,
        titleContentColor = c.ink,
        textContentColor = c.ink,
        title = { Text(title, style = ZeusTheme.type.headline, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(ZeusSpacing.sm), content = body) },
        confirmButton = {
            if (dangerConfirm) SecondaryButton(confirmLabel, onConfirm, enabled = confirmEnabled, danger = true)
            else PrimaryButton(confirmLabel, onConfirm, enabled = confirmEnabled, height = ZeusSpacing.secondaryButtonHeight, shine = false)
        },
        dismissButton = dismissLabel?.let { { SecondaryButton(it, onDismiss) } }
    )
}

/** Empty state with Bolt, a title, message and actions. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    art: @Composable () -> Unit = { BoltMascot(ZeusSizes.mascotMedium + ZeusSpacing.lg) }
) {
    val c = ZeusTheme.colors
    ZeusCard(modifier = modifier.widthIn(max = ZeusSizes.contentMaxWidth).fillMaxWidth(), padding = PaddingValues(ZeusSpacing.xxl)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ZeusSpacing.md)) {
            Box(Modifier.radialGlow(c.glow), contentAlignment = Alignment.Center) { art() }
            Text(title, style = ZeusTheme.type.headline, color = c.ink, textAlign = TextAlign.Center)
            Text(message, style = ZeusTheme.type.body, color = c.muted, textAlign = TextAlign.Center)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(ZeusSpacing.xs))
                PrimaryButton(actionLabel, onAction, icon = actionIcon, leadingIcon = true, modifier = Modifier.fillMaxWidth())
            }
            if (secondaryLabel != null && onSecondary != null) SecondaryButton(secondaryLabel, onSecondary, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ZeusSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(snackbarData = data, shape = ZeusShapes.card, containerColor = c.ink, contentColor = c.canvas, actionColor = c.accent2)
    }
}

/** Gradient-ring avatar with an initial. */
@Composable
fun Avatar(initial: String, modifier: Modifier = Modifier, size: Dp = ZeusSizes.avatar, onClick: (() -> Unit)? = null, contentDescription: String? = null) {
    val c = ZeusTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(ZeusShapes.circle)
            .background(rememberAccentBrush())
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        CappedFontScale(1.2f) { Text(initial.take(1).uppercase(), style = ZeusTheme.type.headline, color = c.onAccent) }
    }
}

/** Thin progress bar; indeterminate when [progress] is null. */
@Composable
fun ZeusLinearProgress(progress: Float? = null, modifier: Modifier = Modifier) {
    val c = ZeusTheme.colors
    if (progress != null) {
        ProgressBar(progress, modifier, height = ZeusSizes.progressThin)
    } else {
        androidx.compose.material3.LinearProgressIndicator(
            modifier = modifier
                .fillMaxWidth()
                .height(ZeusSizes.progressThin)
                .clip(ZeusShapes.bar),
            color = c.accent,
            trackColor = c.tint
        )
    }
}

/** Card with an optional eyebrow title, used to group settings and sheet sections. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ZeusCard(modifier = modifier.fillMaxWidth(), selected = selected, onClick = onClick) {
        if (title != null) Eyebrow(title)
        content()
    }
}

/** Small accent-gradient badge pill ("Zap", "Chapter complete", "3 due"). */
@Composable
fun AccentBadge(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: (() -> Unit)? = null) {
    val c = ZeusTheme.colors
    Row(
        modifier = modifier
            .then(if (onClick != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .heightIn(min = ZeusSizes.iconXl)
            .clip(ZeusShapes.pill)
            .background(rememberAccentBrush())
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = ZeusSpacing.md, vertical = ZeusSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ZeusSpacing.xs)
    ) {
        if (icon != null) Icon(icon, null, tint = c.onAccent, modifier = Modifier.size(ZeusSizes.iconSm))
        Text(text, style = ZeusTheme.type.label, color = c.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
