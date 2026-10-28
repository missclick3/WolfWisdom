package ru.missclick.wolfwisdom.widget

import android.app.Application
import ru.missclick.wolfwisdom.analytics.rememberScreenAnalytics
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.withLock
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.logging.AppLog
import ru.missclick.wolfwisdom.ui.theme.*

data class WidgetPlacementEntry(val id: Int, val state: WidgetState, val quote: WidgetQuote)

data class WidgetPlacementUi(
    val active: Boolean = false,
    val sourceScreen: String = "internal",
    val quote: WidgetQuote? = null,
    val widgets: List<WidgetPlacementEntry> = emptyList(),
    val selectedId: Int? = null,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: Int? = null,
    val create: Boolean = false,
    val complete: Boolean = false
)

/** Uses host membership, never cached quote membership, to identify live instances. */
internal suspend fun placementEntries(context: android.content.Context): List<WidgetPlacementEntry> =
    WidgetUpdates.ids(context).toList().mapNotNull { id ->
        val state = WidgetStateStore(context).read(id) ?: return@mapNotNull null
        WidgetPlacementEntry(id, state, loadWidgetQuote(context, state))
    }

/** Re-read the theme under the same lock used by deletion, restore and rendering. */
internal suspend fun replaceWidgetQuote(context: android.content.Context, id: Int, quoteId: Long): Boolean {
    val committed = WidgetUpdates.mutex.withLock {
        if (id !in WidgetUpdates.ids(context)) return@withLock false
        val store = WidgetStateStore(context)
        val existing = store.read(id) ?: return@withLock false
        val replacement = existing.copy(quoteId = quoteId)
        if (!loadWidgetQuote(context, replacement).hasQuote) return@withLock false
        if (!store.write(id, replacement)) throw java.io.IOException("Widget commit failed")
        true
    }
    return committed && WidgetUpdates.update(context, id)
}

class WidgetPlacementViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(WidgetPlacementUi(
        active = saved["placement_active"] ?: false, sourceScreen = saved["placement_source"] ?: "internal",
        selectedId = saved["selected_widget"]))
    val state = _state.asStateFlow()
    private var refreshJob: Job? = null

    fun open(quoteId: Long?, sourceScreen: String = "internal") {
        if (_state.value.active) return
        saved["placement_source"] = sourceScreen
        saved["quote_id"] = quoteId
        saved["selected_widget"] = null
        saved["placement_active"] = true
        _state.value = WidgetPlacementUi(active = true, sourceScreen = sourceScreen)
        refresh()
    }

    fun dismiss() {
        if (_state.value.submitting) return
        refreshJob?.cancel()
        saved["placement_active"] = false
        saved["selected_widget"] = null
        _state.value = WidgetPlacementUi()
    }

    fun refresh() {
        if (!_state.value.active || _state.value.submitting || _state.value.complete || _state.value.create) return
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val loaded = withContext(Dispatchers.IO) {
                    val quoteId = saved.get<Long>("quote_id")?.takeIf { it > 0 }
                    val binding = WidgetStateStore.initial(quoteId)
                    loadWidgetQuote(context, binding) to placementEntries(context)
                }
                val selected = _state.value.selectedId?.takeIf { id -> loaded.second.any { it.id == id } }
                saved["selected_widget"] = selected
                _state.value = _state.value.copy(quote = loaded.first, widgets = loaded.second,
                    selectedId = selected, loading = false, error = null,
                    create = loaded.second.isEmpty() && loaded.first.hasQuote)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                AppLog.e("FavoriteWidget", "Widget placement loading failed", error)
                _state.value = _state.value.copy(loading = false, error = R.string.widget_load_error)
            }
        }
    }

    fun select(id: Int) {
        if (_state.value.submitting || _state.value.widgets.none { it.id == id }) return
        saved["selected_widget"] = id
        _state.value = _state.value.copy(selectedId = id, error = null)
    }

    fun creationFailed() {
        _state.value = _state.value.copy(create = false, loading = false, error = R.string.widget_create_error)
    }

    fun replace() {
        val draft = _state.value
        val id = draft.selectedId ?: return
        val quoteId = draft.quote?.takeIf { it.hasQuote }?.id ?: return
        if (draft.submitting || draft.loading || draft.complete) return
        refreshJob?.cancel()
        _state.value = draft.copy(submitting = true, error = null)
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) { replaceWidgetQuote(getApplication(), id, quoteId) }
                val remaining = if (success) draft.widgets else withContext(Dispatchers.IO) { placementEntries(getApplication()) }
                _state.value = _state.value.copy(submitting = false, complete = success, widgets = remaining,
                    selectedId = id.takeIf { selected -> remaining.any { it.id == selected } },
                    error = if (success) null else R.string.widget_replace_error)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                AppLog.e("FavoriteWidget", "Widget replacement failed", error)
                _state.value = _state.value.copy(error = R.string.widget_replace_error)
            } finally {
                _state.value = _state.value.copy(submitting = false)
            }
        }
    }
}

/** An overlay of the existing app content; no Activity or navigation destination is added. */
@Composable
internal fun WidgetPlacementHost(model: WidgetPlacementViewModel) {
    val context = LocalContext.current
    val analytics = rememberScreenAnalytics(::WidgetPlacementAnalytics)
    var reported by rememberSaveable { mutableStateOf(false) }
    val state by model.state.collectAsStateWithLifecycle()
    val visible = state.active && !(state.loading && state.widgets.isEmpty()) && !state.create && !state.complete
    LaunchedEffect(state.active, visible) {
        if (!state.active) reported = false
        else if (visible && !reported) {
            analytics.placementOpened(state.sourceScreen)
            reported = true
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refresh() }
    LaunchedEffect(state.complete, state.create) {
        if (state.create) {
            if (WidgetPin.openCreation(context, state.quote?.id)) model.dismiss()
            else model.creationFailed()
        } else if (state.complete) model.dismiss()
    }
    if (state.active) {
        BackHandler { model.dismiss() }
        // Skip even the loading sheet when no host instances exist.
        if (state.loading && state.widgets.isEmpty()) return
        WidgetPlacementContent(state, model::dismiss, {
            analytics.createClicked()
            if (WidgetPin.openCreation(context, state.quote?.id)) model.dismiss()
            else model.creationFailed()
        },
            { id -> analytics.targetClicked(); model.select(id) },
            { analytics.replaceClicked(); model.replace() },
            { analytics.retryClicked(); model.refresh() },
            closeClick = { analytics.closeClicked(); model.dismiss() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetPlacementContent(
    state: WidgetPlacementUi, dismiss: () -> Unit, create: () -> Unit,
    select: (Int) -> Unit, replace: () -> Unit, retry: () -> Unit,
    closeClick: () -> Unit = dismiss
) {
    if (state.create || state.complete) return
    val configuration = LocalConfiguration.current
    if (configuration.smallestScreenWidthDp >= 600) {
        Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnBackPress = !state.submitting, dismissOnClickOutside = !state.submitting)) {
            Surface(Modifier.safeDrawingPadding().padding(24.dp).widthIn(max = 560.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp), color = WolfColors.Surface) {
                PlacementBody(state, closeClick, create, select, replace, retry)
            }
        }
    } else {
        val submitting by rememberUpdatedState(state.submitting)
        // Sheet state is remembered with confirmValueChange as a key. Capturing the
        // entire UI state here recreates it (and replays show()) on every selection.
        val confirmChange: (SheetValue) -> Boolean = remember { { !submitting } }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = confirmChange)
        ModalBottomSheet(onDismissRequest = dismiss,
            modifier = Modifier.testTag("widget-placement-sheet"),
            sheetState = sheetState,
            sheetGesturesEnabled = !state.submitting,
            containerColor = WolfColors.Surface, contentColor = WolfColors.Text,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !state.submitting)) {
            PlacementBody(state, closeClick, create, select, replace, retry)
        }
    }
}

@Composable
private fun PlacementBody(state: WidgetPlacementUi, dismiss: () -> Unit, create: () -> Unit,
                          select: (Int) -> Unit, replace: () -> Unit, retry: () -> Unit) {
    val enabled = !state.loading && !state.submitting && state.quote?.hasQuote == true
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState(), overscrollEffect = null).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.widget_destination_title), style = WolfTypography.Title,
                color = WolfColors.Text, modifier = Modifier.weight(1f))
            IconButton(onClick = dismiss, enabled = !state.submitting) {
                Icon(Icons.Default.Close, stringResource(R.string.widget_close), tint = WolfColors.Secondary)
            }
        }
        state.quote?.let {
            Text(stringResource(R.string.widget_selected_quote, it.text), style = WolfTypography.Body,
                color = WolfColors.Secondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Surface(onClick = create, enabled = enabled, shape = RoundedCornerShape(16.dp),
            color = WolfColors.Pressed, border = BorderStroke(1.dp, WolfColors.GlassBorder)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Add, null, tint = WolfColors.Accent)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.widget_create_new), style = WolfTypography.Body, color = WolfColors.Text)
                    Text(stringResource(R.string.widget_add_to_home), style = WolfTypography.Caption, color = WolfColors.Secondary)
                }
                Icon(Icons.Default.ChevronRight, null, tint = WolfColors.Secondary)
            }
        }
        if (state.loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        if (state.widgets.isNotEmpty()) {
            Text(stringResource(R.string.widget_replace_existing), style = WolfTypography.Label, color = WolfColors.Secondary)
            val rowHeight = with(LocalDensity.current) {
                WolfTypography.Title.lineHeight.toDp() * 2 + WolfTypography.Caption.lineHeight.toDp()
            } + 32.dp
            val visibleRows = minOf(3, state.widgets.size)
            Column(Modifier.testTag("widget-instance-list")
                .height(rowHeight * visibleRows + 8.dp * (visibleRows - 1))
                .verticalScroll(rememberScrollState()).selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.widgets.forEach { entry ->
                    val selected = state.selectedId == entry.id
                    Surface(modifier = Modifier.fillMaxWidth().height(rowHeight).testTag("widget-instance-${entry.id}").selectable(selected, enabled = enabled,
                        role = Role.RadioButton, onClick = { select(entry.id) }),
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) WolfColors.Pressed else WolfColors.Glass,
                        border = BorderStroke(1.dp, if (selected) WolfColors.Accent else WolfColors.GlassBorder)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val theme = entry.state.theme
                            Surface(shape = RoundedCornerShape(12.dp), color = when (theme) {
                                WidgetTheme.DARK -> WolfColors.Background
                                WidgetTheme.LIGHT -> WolfPalette.Moon100
                                WidgetTheme.ANDROID -> MaterialTheme.colorScheme.primaryContainer
                            }) {
                                Icon(when (theme) {
                                    WidgetTheme.DARK -> Icons.Default.DarkMode
                                    WidgetTheme.LIGHT -> Icons.Default.LightMode
                                    WidgetTheme.ANDROID -> Icons.Default.Palette
                                }, null, modifier = Modifier.padding(12.dp).size(24.dp),
                                    tint = when (theme) {
                                        WidgetTheme.DARK -> WolfColors.Accent
                                        WidgetTheme.LIGHT -> WolfPalette.Night800
                                        WidgetTheme.ANDROID -> MaterialTheme.colorScheme.onPrimaryContainer
                                    })
                            }
                            Column(Modifier.weight(1f)) {
                                Text(entry.quote.text, style = WolfTypography.Title, color = WolfColors.Text,
                                    minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(stringResource(when (theme) {
                                    WidgetTheme.DARK -> R.string.widget_placement_theme_dark
                                    WidgetTheme.LIGHT -> R.string.widget_placement_theme_light
                                    WidgetTheme.ANDROID -> R.string.widget_placement_theme_android
                                }), style = WolfTypography.Caption, color = WolfColors.Secondary)
                            }
                            RadioButton(selected, onClick = null, enabled = enabled)
                        }
                    }
                }
            }
        }
        state.error?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = retry, enabled = !state.submitting) { Text(stringResource(R.string.widget_retry)) }
        }
        if (state.widgets.isNotEmpty()) {
            Text(stringResource(R.string.widget_replace_hint), style = WolfTypography.Caption, color = WolfColors.Secondary)
            Button(onClick = replace, enabled = enabled && state.selectedId != null,
                shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(stringResource(if (state.submitting) R.string.widget_replacing else R.string.widget_replace))
            }
        }
    }
}
