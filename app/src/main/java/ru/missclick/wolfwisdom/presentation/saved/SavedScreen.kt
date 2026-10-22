package ru.missclick.wolfwisdom.presentation.saved

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.domain.model.Quote
import ru.missclick.wolfwisdom.presentation.components.WolfSnackbarHost
import ru.missclick.wolfwisdom.presentation.components.WolfWisdomScreen
import ru.missclick.wolfwisdom.presentation.components.showWolfSnackbar
import ru.missclick.wolfwisdom.presentation.saved.components.SavedEmptyState
import ru.missclick.wolfwisdom.presentation.saved.components.SavedHeader
import ru.missclick.wolfwisdom.presentation.saved.components.SavedQuoteCard
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@Composable
fun SavedRoot(
    viewModel: SavedViewModel = koinViewModel(),
    onBackClick: () -> Unit = {},
    onShareClick: (Quote, Int) -> Unit = { _, _ -> },
    onWidgetClick: (Quote) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, lifecycleOwner, resources) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is SavedEffect.ShowError -> snackbarHostState.showWolfSnackbar(
                        headline = resources.getString(effect.messageRes)
                    )
                }
            }
        }
    }

    BackHandler(enabled = state.isSearchVisible) {
        keyboard?.hide()
        viewModel.onAction(SavedAction.CloseSearch)
    }

    SavedScreen(
        state = state,
        onAction = viewModel::onAction,
        onBackClick = {
            keyboard?.hide()
            onBackClick()
        },
        onShareClick = onShareClick,
        onWidgetClick = onWidgetClick,
        snackbarHost = { WolfSnackbarHost(snackbarHostState) }
    )
}

@Composable
fun SavedScreen(
    state: SavedState,
    onAction: (SavedAction) -> Unit,
    onBackClick: () -> Unit = {},
    onShareClick: (Quote, Int) -> Unit = { _, _ -> },
    onWidgetClick: (Quote) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {}
) {
    val listState = rememberLazyGridState()
    val layoutDirection = LocalLayoutDirection.current
    val contentWindowInsets = ScaffoldDefaults.contentWindowInsets
    val windowPadding = contentWindowInsets.asPaddingValues()
    val headerSideInset = maxOf(
        windowPadding.calculateLeftPadding(layoutDirection),
        windowPadding.calculateRightPadding(layoutDirection)
    )
    val deviceConfiguration = currentDeviceConfiguration()
    val hasTwoColumns = deviceConfiguration != DeviceConfiguration.MOBILE_PORTRAIT
    val columns = if (hasTwoColumns) 2 else 1
    val headerDimensions = savedHeaderDimensions()
    val collapseProgress = rememberSavedHeaderProgress(
        listState, columns, headerDimensions.collapseDistance
    )
    val expandedHeaderSpace = headerDimensions.expandedHeight - headerDimensions.collapsedHeight
    val horizontalPadding = when (deviceConfiguration) {
        DeviceConfiguration.TABLET_PORTRAIT -> WolfSpacing.Space24
        DeviceConfiguration.TABLET_LANDSCAPE -> WolfSpacing.Space32
        else -> 6.dp
    }
    val topPadding = when (deviceConfiguration) {
        DeviceConfiguration.TABLET_PORTRAIT -> 88.dp
        DeviceConfiguration.TABLET_LANDSCAPE -> 56.dp
        else -> WolfSpacing.Space16
    }
    // Keep each quote's artwork stable when an earlier card is removed.
    val sceneIndices = remember { mutableMapOf<Long, Int>() }
    state.quotes.forEach { quote ->
        sceneIndices.getOrPut(quote.id) { sceneIndices.size }
    }

    WolfWisdomScreen {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize().imePadding(),
                containerColor = Color.Transparent,
                contentWindowInsets = contentWindowInsets,
                topBar = {
                    // Reserve only the compact bar. The expanded part overlays a fixed
                    // grid viewport, so changing header size never changes scroll geometry.
                    Box(Modifier.statusBarsPadding().height(headerDimensions.collapsedHeight))
                },
                snackbarHost = snackbarHost
            ) { innerPadding ->
                // Mirror the larger safe inset so a landscape cutout or navigation bar
                // cannot shift the grid away from the physical center of the screen.
                val sideInset = maxOf(
                    innerPadding.calculateLeftPadding(layoutDirection),
                    innerPadding.calculateRightPadding(layoutDirection)
                )
                AnimatedContent(
                    targetState = state,
                    modifier = Modifier.fillMaxSize().padding(
                        start = sideInset,
                        end = sideInset,
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    ).drawWithContent {
                        // Clip cards below the current header without resizing/re-padding
                        // the grid. The full-screen background remains untouched.
                        clipRect(top = expandedHeaderSpace.toPx() * (1f - collapseProgress.value)) {
                            this@drawWithContent.drawContent()
                        }
                    },
                    contentKey = { contentState ->
                        when {
                            contentState.isLoading -> "loading"
                            contentState.visibleQuotes.isEmpty() -> "empty"
                            else -> "quotes"
                        }
                    },
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                    label = "Saved content"
                ) { contentState ->
                    val visibleQuotes = remember(
                        contentState.quotes, contentState.searchQuery, contentState.isSearchVisible
                    ) { contentState.visibleQuotes }
                    Box(Modifier.fillMaxSize()) {
                        when {
                            contentState.isLoading -> CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = WolfColors.Accent
                            )
                            visibleQuotes.isEmpty() -> SavedEmptyState(
                                isSearchResult = contentState.quotes.isNotEmpty(),
                                modifier = Modifier.align(Alignment.Center).padding(
                                    start = WolfSpacing.Space20,
                                    end = WolfSpacing.Space20,
                                    bottom = WolfSpacing.Space32
                                )
                            )
                            else -> {
                                val artwork = ImageBitmap.imageResource(R.drawable.saved_landscapes)
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(columns),
                                    modifier = Modifier.fillMaxSize(),
                                    state = listState,
                                    contentPadding = PaddingValues(
                                        start = horizontalPadding,
                                        end = horizontalPadding,
                                        top = topPadding + expandedHeaderSpace,
                                        bottom = WolfSpacing.Space16
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(if (hasTwoColumns) WolfSpacing.Space16 else WolfSpacing.Space8),
                                    horizontalArrangement = Arrangement.spacedBy(WolfSpacing.Space16)
                                ) {
                                    items(visibleQuotes, key = { it.id }) { quote ->
                                        SavedQuoteCard(
                                            quote = quote,
                                            artwork = artwork,
                                            sceneIndex = sceneIndices.getValue(quote.id),
                                            isRemoving = quote.id in contentState.removingQuoteIds,
                                            onRemoveClick = { onAction(SavedAction.RemoveQuote(quote.id)) },
                                            onShareClick = { onShareClick(quote, sceneIndices.getValue(quote.id)) },
                                            onWidgetClick = { onWidgetClick(quote) },
                                            scaleToWidth = hasTwoColumns,
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = tween(180),
                                                placementSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                ),
                                                fadeOutSpec = tween(180)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            SavedHeader(
                state = state,
                onAction = onAction,
                onBackClick = onBackClick,
                modifier = Modifier.padding(horizontal = headerSideInset).statusBarsPadding(),
                horizontalPadding = horizontalPadding,
                dimensions = headerDimensions,
                collapseProgress = { collapseProgress.value }
            )
        }
    }
}

private val PreviewQuotes = listOf(
    Quote(1, "Волк не тот, кто волк, а тот, кто волк.", true),
    Quote(2, "Тишина — это тоже ответ.", true),
    Quote(3, "Не бойся идти один. Бойся жить не своей жизнью.", true),
    Quote(4, "Сила — в спокойствии, а не в шуме.", true)
)

@Preview(name = "Saved quotes", widthDp = 390, heightDp = 844, locale = "ru")
@Preview(name = "Phone landscape", widthDp = 844, heightDp = 390, locale = "ru")
@Preview(name = "Tablet portrait", widthDp = 834, heightDp = 1112, locale = "ru")
@Preview(name = "Tablet landscape", widthDp = 1112, heightDp = 834, locale = "ru")
@Composable
private fun Preview() {
    WolfWisdomTheme {
        SavedScreen(
            state = SavedState(quotes = PreviewQuotes, isLoading = false),
            onAction = {}
        )
    }
}

@Preview(name = "Empty collection", widthDp = 390, heightDp = 844, locale = "ru")
@Composable
private fun EmptyPreview() {
    WolfWisdomTheme {
        SavedScreen(state = SavedState(isLoading = false), onAction = {})
    }
}

@Preview(name = "Search results", widthDp = 390, heightDp = 844, locale = "ru")
@Composable
private fun SearchPreview() {
    WolfWisdomTheme {
        SavedScreen(
            state = SavedState(PreviewQuotes, isLoading = false, isSearchVisible = true, searchQuery = "тишина"),
            onAction = {}
        )
    }
}

@Preview(name = "No results", widthDp = 390, heightDp = 844, locale = "ru")
@Composable
private fun NoResultsPreview() {
    WolfWisdomTheme {
        SavedScreen(
            state = SavedState(PreviewQuotes, isLoading = false, isSearchVisible = true, searchQuery = "рассвет"),
            onAction = {}
        )
    }
}
