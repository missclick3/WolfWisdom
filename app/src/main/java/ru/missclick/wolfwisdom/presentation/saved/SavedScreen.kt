package ru.missclick.wolfwisdom.presentation.saved

import android.content.Intent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
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
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@Composable
fun SavedRoot(
    viewModel: SavedViewModel = koinViewModel(),
    onBackClick: () -> Unit = {},
    onWidgetClick: (Quote) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, lifecycleOwner, context) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collectLatest { effect ->
                when (effect) {
                    is SavedEffect.ShowError -> snackbarHostState.showWolfSnackbar(
                        headline = context.getString(effect.messageRes)
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
        onShareClick = { quote ->
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, quote.quote)
            }, null))
        },
        onWidgetClick = onWidgetClick,
        snackbarHost = { WolfSnackbarHost(snackbarHostState) }
    )
}

@Composable
fun SavedScreen(
    state: SavedState,
    onAction: (SavedAction) -> Unit,
    onBackClick: () -> Unit = {},
    onShareClick: (Quote) -> Unit = {},
    onWidgetClick: (Quote) -> Unit = {},
    snackbarHost: @Composable () -> Unit = {}
) {
    val listState = rememberLazyListState()
    // Keep each quote's artwork stable when an earlier card is removed.
    val sceneIndices = remember { mutableMapOf<Long, Int>() }
    state.quotes.forEach { quote ->
        sceneIndices.getOrPut(quote.id) { sceneIndices.size }
    }

    WolfWisdomScreen {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = Color.Transparent,
            topBar = {
                SavedHeader(
                    state = state,
                    onAction = onAction,
                    onBackClick = onBackClick,
                    modifier = Modifier.statusBarsPadding()
                )
            },
            snackbarHost = snackbarHost
        ) { innerPadding ->
            AnimatedContent(
                targetState = state,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
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
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(
                                    start = 6.dp,
                                    end = 6.dp,
                                    top = WolfSpacing.Space16,
                                    bottom = WolfSpacing.Space16
                                ),
                                verticalArrangement = Arrangement.spacedBy(WolfSpacing.Space8)
                            ) {
                                items(visibleQuotes, key = { it.id }) { quote ->
                                    SavedQuoteCard(
                                        quote = quote,
                                        artwork = artwork,
                                        sceneIndex = sceneIndices.getValue(quote.id),
                                        isRemoving = quote.id in contentState.removingQuoteIds,
                                        onRemoveClick = { onAction(SavedAction.RemoveQuote(quote.id)) },
                                        onShareClick = { onShareClick(quote) },
                                        onWidgetClick = { onWidgetClick(quote) },
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
    }
}

private val PreviewQuotes = listOf(
    Quote(1, "Волк не тот, кто волк, а тот, кто волк.", true),
    Quote(2, "Тишина — это тоже ответ.", true),
    Quote(3, "Не бойся идти один. Бойся жить не своей жизнью.", true),
    Quote(4, "Сила — в спокойствии, а не в шуме.", true)
)

@Preview(name = "Saved quotes", widthDp = 390, heightDp = 844, locale = "ru")
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
