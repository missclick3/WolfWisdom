package ru.missclick.wolfwisdom.presentation.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.ui.theme.*

enum class WolfMenuAction(@StringRes val labelRes: Int, @DrawableRes internal val iconRes: Int) {
    SAVED(R.string.saved, R.drawable.drawer_heart),
    ACHIEVEMENTS(R.string.achievements, R.drawable.drawer_achievements),
    SUPPORT(R.string.donate, R.drawable.ic_menu_support),
    CONTACTS(R.string.contacts, R.drawable.ic_menu_contacts),
    ABOUT(R.string.about_app, R.drawable.ic_menu_about),
    SETTINGS(R.string.settings, R.drawable.drawer_settings)
}

private const val DrawerAnimationMillis = 320
private val DrawerBackground = Color(0xFF09131C)

/** Keeps the modal alive through its exit, then delivers the selected destination. */
@Composable
fun WolfNavigationDrawer(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onItemClick: (WolfMenuAction) -> Unit = {},
    brandTopInWindowPx: Float? = null
) {
    val isMobileLandscape = currentDeviceConfiguration() == DeviceConfiguration.MOBILE_LANDSCAPE
    val drawerWidth = if (isMobileLandscape) 360.dp else 268.dp
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = expanded
    val transition = rememberTransition(visibility, label = "Navigation drawer")
    val scrimAlpha by transition.animateFloat(
        transitionSpec = { tween(DrawerAnimationMillis) }, label = "Drawer backdrop"
    ) { if (it) 0.48f else 0f }
    var pendingAction by remember { mutableStateOf<WolfMenuAction?>(null) }
    val currentOnItemClick by rememberUpdatedState(onItemClick)
    LaunchedEffect(visibility.isIdle, visibility.currentState, pendingAction) {
        if (visibility.isIdle && !visibility.currentState) {
            pendingAction?.let {
                pendingAction = null
                currentOnItemClick(it)
            }
        }
    }
    if (!visibility.currentState && !visibility.targetState && visibility.isIdle) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val density = LocalDensity.current
        val brandTopPadding = brandTopInWindowPx?.let { with(density) { it.toDp() } }
            ?: (WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + WolfSpacing.Space16)
        val window = (LocalView.current.parent as DialogWindowProvider).window
        SideEffect {
            // The animated scrim below owns dimming, including behind the system bars.
            window.setDimAmount(0f)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
        BoxWithConstraints(modifier.fillMaxSize()) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = stringResource(R.string.close_options),
                    onClick = onDismissRequest
                ))
            transition.AnimatedVisibility(
                visible = { it },
                enter = slideInHorizontally(tween(DrawerAnimationMillis, easing = FastOutSlowInEasing)) { -it },
                exit = slideOutHorizontally(tween(DrawerAnimationMillis, easing = FastOutSlowInEasing)) { -it }
            ) {
                DrawerContent(
                    // Leave room for an outside tap while accommodating landscape display cutouts.
                    modifier = Modifier.width(minOf(drawerWidth, (maxWidth - 48.dp).coerceAtLeast(1.dp)))
                        .fillMaxHeight(),
                    enabled = expanded && pendingAction == null,
                    singleLineText = isMobileLandscape,
                    brandTopPadding = brandTopPadding,
                    onItemClick = { pendingAction = it; onDismissRequest() }
                )
            }
        }
    }
}

@Composable
private fun DrawerContent(
    enabled: Boolean,
    onItemClick: (WolfMenuAction) -> Unit,
    modifier: Modifier = Modifier,
    singleLineText: Boolean = false,
    brandTopPadding: Dp = WolfSpacing.Space16
) {
    val title = stringResource(R.string.app_title)
    Box(modifier.testTag("navigation_drawer")
        .background(DrawerBackground)
        .semantics { paneTitle = title }
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        Image(
            painterResource(R.drawable.drawer_forest), null,
            modifier = Modifier.matchParentSize().blur(1.5.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to DrawerBackground.copy(alpha = 0.96f),
            0.65f to DrawerBackground.copy(alpha = 0.92f),
            1f to Color(0xFF060F18).copy(alpha = 0.25f)
        )))
        Column(Modifier.fillMaxSize().windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        )
            .verticalScroll(rememberScrollState())
            .padding(start = WolfSpacing.Space20, end = WolfSpacing.Space20,
                top = brandTopPadding, bottom = WolfSpacing.Space16)) {
            Row(Modifier.fillMaxWidth().heightIn(min = WolfSpacing.Space48), verticalAlignment = Alignment.CenterVertically) {
                WolfLogoBrand(contentDescription = null)
                Spacer(Modifier.width(WolfSpacing.Space12))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = WolfTypography.Title.copy(fontSize = 17.sp, lineHeight = 23.sp),
                        maxLines = if (singleLineText) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                        color = WolfColors.Text)
                    Text(stringResource(R.string.app_subtitle),
                        style = WolfTypography.Body.copy(fontSize = 9.sp, lineHeight = 12.sp),
                        maxLines = if (singleLineText) 1 else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                        color = WolfColors.Muted)
                }
            }
            Spacer(Modifier.height(WolfSpacing.Space24))
            WolfMenuAction.entries.filter { it != WolfMenuAction.SETTINGS }.forEach { action ->
                DrawerRow(action, enabled, singleLineText) { onItemClick(action) }
            }
            Spacer(Modifier.height(WolfSpacing.Space16))
            HorizontalDivider(thickness = WolfLayout.Hairline, color = WolfColors.Border)
            Spacer(Modifier.height(WolfSpacing.Space16))
            DrawerRow(WolfMenuAction.SETTINGS, enabled, singleLineText) { onItemClick(WolfMenuAction.SETTINGS) }
        }
    }
}

@Composable
private fun DrawerRow(action: WolfMenuAction, enabled: Boolean, singleLineText: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = WolfSpacing.Space48)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(start = WolfSpacing.Space8, top = WolfSpacing.Space12, bottom = WolfSpacing.Space12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space16)) {
        // Preserve the source icon viewports; the design uses a 20dp leading slot.
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(action.iconRes), null, tint = Color.Unspecified)
        }
        Text(stringResource(action.labelRes), Modifier.weight(1f),
            style = WolfTypography.Body.copy(fontSize = 13.sp, lineHeight = 18.sp),
            maxLines = if (singleLineText) 1 else Int.MAX_VALUE,
            color = WolfColors.Text, overflow = TextOverflow.Ellipsis)
        Box(Modifier.size(12.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.drawer_chevron), null, tint = Color.Unspecified)
        }
    }
}

@Preview(name = "Navigation drawer", widthDp = 268, heightDp = 844, locale = "ru")
@Preview(name = "Navigation drawer, short screen", widthDp = 268, heightDp = 390, locale = "en")
@Composable
private fun DrawerPreview() {
    WolfWisdomTheme {
        DrawerContent(enabled = true, onItemClick = {}, modifier = Modifier.fillMaxSize())
    }
}
