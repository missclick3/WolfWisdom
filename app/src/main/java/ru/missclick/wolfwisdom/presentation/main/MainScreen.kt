package ru.missclick.wolfwisdom.presentation.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.components.FlippingQuoteCard
import ru.missclick.wolfwisdom.presentation.components.quoteSwipeGestures
import ru.missclick.wolfwisdom.presentation.components.rememberQuoteSwipeState
import ru.missclick.wolfwisdom.presentation.components.SwipeUpHint
import ru.missclick.wolfwisdom.presentation.components.WolfHeader
import ru.missclick.wolfwisdom.presentation.components.WolfMenuAction
import ru.missclick.wolfwisdom.presentation.components.WolfWisdomScreen
import ru.missclick.wolfwisdom.presentation.components.WolfSnackbarHost
import ru.missclick.wolfwisdom.presentation.components.showWolfSnackbar
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

private const val PreviewQuote = "Мечтай масштабно. Делай по силам. Спи регулярно."

@Composable
fun MainRoot(
    viewModel: MainViewModel = koinViewModel(),
    onMenuItemClick: (WolfMenuAction) -> Unit = {},
    onShareClick: (String) -> Unit = {},
    onWidgetClick: () -> Unit = {},
    onNavigateToSaved: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    val closeHeadline = stringResource(R.string.back_to_exit_headline)
    val closeDetail = stringResource(R.string.back_to_exit_detail)

    LaunchedEffect(viewModel, lifecycleOwner, closeHeadline, closeDetail) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    MainEffect.ShowCloseSnackbar -> snackbarHostState.showWolfSnackbar(
                        headline = closeHeadline,
                        detail = closeDetail,
                        durationMillis = 2_000L
                    )
                }
            }
        }
    }

    BackHandler(enabled = !state.isOptionsVisible) {
        if (snackbarHostState.currentSnackbarData != null) {
            activity?.finish()
        } else {
            viewModel.onAction(MainAction.BackPressed)
        }
    }

    MainScreen(
        state = state,
        onAction = viewModel::onAction,
        onMenuItemClick = { action ->
            if (action == WolfMenuAction.SAVED) {
                onNavigateToSaved()
            } else {
                onMenuItemClick(action)
            }
        },
        onShareClick = { onShareClick(state.quote) },
        onWidgetClick = onWidgetClick,
        snackbarHost = { WolfSnackbarHost(hostState = snackbarHostState) }
    )
}

@Composable
fun MainScreen(
    state: MainState,
    onAction: (MainAction) -> Unit,
    onMenuItemClick: (WolfMenuAction) -> Unit = {},
    onShareClick: () -> Unit = {},
    onWidgetClick: () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
) {
    val swipeState = rememberQuoteSwipeState()
    WolfWisdomScreen {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = snackbarHost
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .quoteSwipeGestures(
                        state = swipeState,
                        enabled = state.quote.isNotEmpty() && !state.isNavigating && !state.isSavingQuote &&
                            state.quoteTransition == null && !state.isOptionsVisible,
                        canGoBack = state.canGoBack,
                        onSwipeUp = { onAction(MainAction.SwipeUp) },
                        onSwipeDown = { onAction(MainAction.SwipeDown) }
                    )
            ) {
                WolfHeader(
                    isOptionsVisible = state.isOptionsVisible,
                    onOptionsClick = { onAction(MainAction.ToggleOptions) },
                    onOptionsDismissRequest = { onAction(MainAction.DismissOptions) },
                    onMenuItemClick = onMenuItemClick
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
                    if (state.quote.isNotEmpty()) {
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
                            onShareClick = onShareClick,
                            onWidgetClick = onWidgetClick
                        )
                    }
                }
                SwipeUpHint(
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
}

@Preview
@Composable
private fun Preview() {
    WolfWisdomTheme {
        MainScreen(
            state = MainState(quote = PreviewQuote),
            onAction = {}
        )
    }
}
