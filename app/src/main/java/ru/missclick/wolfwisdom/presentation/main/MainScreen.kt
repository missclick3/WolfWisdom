package ru.missclick.wolfwisdom.presentation.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.components.FlippingQuoteCard
import ru.missclick.wolfwisdom.presentation.components.QuoteActionButtons
import ru.missclick.wolfwisdom.presentation.components.QuoteSwipeState
import ru.missclick.wolfwisdom.presentation.components.quoteSwipeGestures
import ru.missclick.wolfwisdom.presentation.components.rememberQuoteSwipeState
import ru.missclick.wolfwisdom.presentation.components.SwipeUpHint
import ru.missclick.wolfwisdom.presentation.components.WolfNavigationDrawer
import ru.missclick.wolfwisdom.presentation.components.WolfHeader
import ru.missclick.wolfwisdom.presentation.components.WolfMenuAction
import ru.missclick.wolfwisdom.presentation.components.WolfWisdomScreen
import ru.missclick.wolfwisdom.presentation.components.WolfSnackbarHost
import ru.missclick.wolfwisdom.presentation.components.WolfLoadingState
import ru.missclick.wolfwisdom.presentation.components.showWolfSnackbar
import ru.missclick.wolfwisdom.presentation.components.showWolfErrorSnackbar
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.error.LoadErrorNavigationEffect
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

private const val PreviewQuote = "Мечтай масштабно. Делай по силам. Спи регулярно."

@Composable
fun MainRoot(
    viewModel: MainViewModel = koinViewModel(),
    onShareClick: (String) -> Unit = {},
    onWidgetClick: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {},
    onNavigateToAchievements: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onNavigateToContacts: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLoadError: () -> Unit = {}
) {
    LoadErrorNavigationEffect(viewModel.errorHandler, onLoadError)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    val closeHeadline = stringResource(R.string.back_to_exit_headline)
    val closeDetail = stringResource(R.string.back_to_exit_detail)
    val errorDetail = stringResource(R.string.error_snackbar_detail)
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel, lifecycleOwner, closeHeadline, closeDetail, resources, errorDetail) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    MainEffect.ShowCloseSnackbar -> snackbarHostState.showWolfSnackbar(
                        headline = closeHeadline,
                        detail = closeDetail,
                        durationMillis = 2_000L
                    )
                    is MainEffect.ShowError -> snackbarHostState.showWolfErrorSnackbar(
                        headline = resources.getString(effect.messageRes),
                        detail = errorDetail
                    )
                }
            }
        }
    }

    MainScreen(
        state = state,
        onAction = { action ->
            when (action) {
                MainAction.BackPressed -> {
                    if (snackbarHostState.currentSnackbarData?.visuals?.message == closeHeadline) {
                        activity?.finish()
                    } else {
                        viewModel.onAction(action)
                    }
                }
                MainAction.NavigateToSaved -> onNavigateToSaved()
                MainAction.NavigateToAchievements -> onNavigateToAchievements()
                MainAction.SupportClick -> {
                    try {
                        onSupportClick()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        scope.launch {
                            snackbarHostState.showWolfErrorSnackbar(
                                headline = resources.getString(R.string.support_open_error),
                                detail = errorDetail
                            )
                        }
                    }
                }
                MainAction.NavigateToContacts -> onNavigateToContacts()
                MainAction.NavigateToAbout -> onNavigateToAbout()
                MainAction.NavigateToSettings -> onNavigateToSettings()
                MainAction.ShareClick -> onShareClick(state.quote)
                MainAction.WidgetClick -> onWidgetClick()
                else -> viewModel.onAction(action)
            }
        },
        snackbarHost = { WolfSnackbarHost(hostState = snackbarHostState) }
    )
}

@Composable
fun MainScreen(
    state: MainState,
    onAction: (MainAction) -> Unit,
    snackbarHost: @Composable () -> Unit = {},
) {
    val swipeState = rememberQuoteSwipeState()
    val deviceConfiguration = currentDeviceConfiguration()
    var headerBrandTopInWindowPx by remember { mutableStateOf<Float?>(null) }
    val onBrandPositioned: (LayoutCoordinates) -> Unit = {
        headerBrandTopInWindowPx = it.positionInWindow().y
    }
    BackHandler(enabled = !state.isOptionsVisible) {
        onAction(MainAction.BackPressed)
    }
    WolfWisdomScreen {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost
        ) { innerPadding ->
            if (deviceConfiguration != DeviceConfiguration.MOBILE_PORTRAIT) {
                AdaptiveMainContent(
                    state = state,
                    deviceConfiguration = deviceConfiguration,
                    swipeState = swipeState,
                    onAction = onAction,
                    onBrandPositioned = onBrandPositioned,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .quoteSwipeGestures(
                            state = swipeState,
                            enabled = !state.showLoading && state.quote.isNotEmpty() && !state.isNavigating && !state.isSavingQuote &&
                                state.quoteTransition == null && !state.isOptionsVisible,
                            canGoBack = state.canGoBack,
                            onSwipeUp = { onAction(MainAction.SwipeUp) },
                            onSwipeDown = { onAction(MainAction.SwipeDown) }
                        )
                ) {
                    WolfHeader(
                        horizontalPadding = WolfSpacing.Space20,
                        isOptionsVisible = state.isOptionsVisible,
                        onBrandPositioned = onBrandPositioned,
                        onOptionsClick = { onAction(MainAction.ToggleOptions) }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(
                                horizontal = WolfSpacing.Space20,
                                vertical = WolfSpacing.Space32
                            ),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!state.showLoading && state.quote.isNotEmpty()) {
                            FlippingQuoteCard(
                                quote = state.quote,
                                isSaved = state.isQuoteSaved,
                                swipeState = swipeState,
                                transition = state.quoteTransition,
                                enabled = !state.isNavigating && !state.isSavingQuote,
                                onSwipeUp = { onAction(MainAction.SwipeUp) },
                                onSwipeDown = { onAction(MainAction.SwipeDown) },
                                onAnimationFinished = { onAction(MainAction.QuoteAnimationFinished(it)) },
                                onSaveClick = { onAction(MainAction.ToggleQuoteSaved) },
                                onShareClick = { onAction(MainAction.ShareClick) },
                                onWidgetClick = { onAction(MainAction.WidgetClick) }
                            )
                        }
                    }
                    SwipeUpHint(
                        isVisible = !state.showLoading && state.quote.isNotEmpty() && !state.isOptionsVisible,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(
                                start = WolfSpacing.Space20,
                                end = WolfSpacing.Space20,
                                bottom = WolfSpacing.Space24
                            )
                    )
                }
            }
        }
        if (state.showLoading) WolfLoadingState()
        WolfNavigationDrawer(
            expanded = state.isOptionsVisible,
            brandTopInWindowPx = headerBrandTopInWindowPx,
            onDismissRequest = { onAction(MainAction.DismissOptions) },
            onItemClick = { onAction(it.toMainAction()) }
        )
    }
}

@Composable
private fun AdaptiveMainContent(
    state: MainState,
    deviceConfiguration: DeviceConfiguration,
    swipeState: QuoteSwipeState,
    onAction: (MainAction) -> Unit,
    onBrandPositioned: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPhoneLandscape = deviceConfiguration == DeviceConfiguration.MOBILE_LANDSCAPE
    val actionEndPadding = if (isPhoneLandscape) WolfSpacing.Space8 else WolfSpacing.Space16
    val quoteWidth = when (deviceConfiguration) {
        DeviceConfiguration.TABLET_PORTRAIT -> 560.dp
        DeviceConfiguration.TABLET_LANDSCAPE -> 504.dp
        else -> 420.dp
    }
    val quoteHeight = when (deviceConfiguration) {
        DeviceConfiguration.TABLET_PORTRAIT -> 276.dp
        DeviceConfiguration.TABLET_LANDSCAPE -> 242.dp
        else -> 170.dp
    }
    val canInteract = !state.showLoading && state.quote.isNotEmpty() && !state.isNavigating && !state.isSavingQuote &&
        state.quoteTransition == null && !state.isOptionsVisible && !swipeState.isDragging &&
        !swipeState.isAwaitingNavigation && !swipeState.isSettling

    val swipeModifier = Modifier.quoteSwipeGestures(
        state = swipeState,
        enabled = !state.showLoading && state.quote.isNotEmpty() && !state.isNavigating && !state.isSavingQuote &&
            state.quoteTransition == null && !state.isOptionsVisible,
        canGoBack = state.canGoBack,
        onSwipeUp = { onAction(MainAction.SwipeUp) },
        onSwipeDown = { onAction(MainAction.SwipeDown) }
    )
    BoxWithConstraints(modifier = modifier.then(swipeModifier)) {
        WolfHeader(
            modifier = Modifier.align(Alignment.TopCenter),
            compact = true,
            horizontalPadding = if (isPhoneLandscape) WolfSpacing.Space16 else WolfSpacing.Space24,
            endPadding = actionEndPadding,
            isOptionsVisible = state.isOptionsVisible,
            onBrandPositioned = onBrandPositioned,
            onOptionsClick = { onAction(MainAction.ToggleOptions) }
        )
        if (!state.showLoading && state.quote.isNotEmpty()) {
            // Reserve matching space on both sides so the card stays centered on the screen.
            // The bounded area keeps long quotes clear of the header and hint.
            Column(
                modifier = Modifier.align(Alignment.Center)
                    .offset(y = if (deviceConfiguration == DeviceConfiguration.TABLET_PORTRAIT) 32.dp else 0.dp)
                    .width(minOf(quoteWidth, (maxWidth - 192.dp).coerceAtLeast(1.dp)))
                    .heightIn(max = (maxHeight - 160.dp).coerceAtLeast(1.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                FlippingQuoteCard(
                    // Handle drags inside the scroll container before it consumes them.
                    // The surrounding screen uses the same state and callbacks.
                    modifier = swipeModifier,
                    quote = state.quote,
                    isSaved = state.isQuoteSaved,
                    swipeState = swipeState,
                    transition = state.quoteTransition,
                    enabled = !state.isNavigating && !state.isSavingQuote,
                    onSwipeUp = { onAction(MainAction.SwipeUp) },
                    onSwipeDown = { onAction(MainAction.SwipeDown) },
                    onAnimationFinished = { onAction(MainAction.QuoteAnimationFinished(it)) },
                    onSaveClick = { onAction(MainAction.ToggleQuoteSaved) },
                    onShareClick = { onAction(MainAction.ShareClick) },
                    onWidgetClick = { onAction(MainAction.WidgetClick) },
                    showActions = false,
                    minHeight = quoteHeight,
                    quoteStyle = WolfTypography.Quote.copy(
                        fontSize = if (isPhoneLandscape) 26.sp else 36.sp,
                        lineHeight = if (isPhoneLandscape) 32.sp else 44.sp,
                        lineBreak = LineBreak.Heading
                    )
                )
            }
            val actionsModifier = if (isPhoneLandscape) {
                Modifier.align(Alignment.BottomEnd).padding(end = actionEndPadding)
            } else {
                Modifier.align(Alignment.CenterEnd)
                    .padding(end = actionEndPadding)
                    .offset(y = if (deviceConfiguration == DeviceConfiguration.TABLET_PORTRAIT) 160.dp else 96.dp)
            }
            Column(
                modifier = actionsModifier
                    .heightIn(max = (maxHeight - 112.dp).coerceAtLeast(1.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                QuoteActionButtons(
                    isSaved = state.isQuoteSaved,
                    enabled = canInteract,
                    onSaveClick = { onAction(MainAction.ToggleQuoteSaved) },
                    onShareClick = { onAction(MainAction.ShareClick) },
                    onWidgetClick = { onAction(MainAction.WidgetClick) }
                )
            }
        }
        SwipeUpHint(
            isVisible = !state.showLoading && state.quote.isNotEmpty() && !state.isOptionsVisible,
            modifier = Modifier.align(Alignment.BottomCenter).padding(
                horizontal = WolfSpacing.Space20,
                vertical = if (isPhoneLandscape) WolfSpacing.Space4 else WolfSpacing.Space24
            )
        )
    }
}

private fun WolfMenuAction.toMainAction(): MainAction = when (this) {
    WolfMenuAction.SAVED -> MainAction.NavigateToSaved
    WolfMenuAction.ACHIEVEMENTS -> MainAction.NavigateToAchievements
    WolfMenuAction.SUPPORT -> MainAction.SupportClick
    WolfMenuAction.CONTACTS -> MainAction.NavigateToContacts
    WolfMenuAction.ABOUT -> MainAction.NavigateToAbout
    WolfMenuAction.SETTINGS -> MainAction.NavigateToSettings
}

@Preview(name = "Phone portrait", widthDp = 390, heightDp = 844, locale = "ru")
@Preview(name = "Phone landscape", widthDp = 844, heightDp = 390, locale = "ru")
@Preview(name = "Tablet portrait", widthDp = 834, heightDp = 1112, locale = "ru")
@Preview(name = "Tablet landscape", widthDp = 1112, heightDp = 834, locale = "ru")
@Composable
private fun Preview() {
    WolfWisdomTheme {
        MainScreen(
            state = MainState(quote = PreviewQuote),
            onAction = {}
        )
    }
}
