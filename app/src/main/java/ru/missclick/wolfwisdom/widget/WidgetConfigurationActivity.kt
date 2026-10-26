package ru.missclick.wolfwisdom.widget

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.*
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ru.missclick.wolfwisdom.R
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.context.GlobalContext
import ru.missclick.wolfwisdom.domain.repository.QuoteRepository
import ru.missclick.wolfwisdom.ui.theme.*

class WidgetConfigurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val manager = AppWidgetManager.getInstance(this)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val pin = intent.getBooleanExtra("request_pin", false)
        if ((!pin && (id == AppWidgetManager.INVALID_APPWIDGET_ID || manager.getAppWidgetInfo(id)?.provider?.className.orEmpty() !in
                    WidgetUpdates.providers.map { it.name })) || (pin && !manager.isRequestPinAppWidgetSupported)) {
            finish(); return
        }
        val model = ViewModelProvider(this)[WidgetConfigViewModel::class.java]
        model.initialize(id, pin, intent.getLongExtra("quote_id", -1L).takeIf { it > 0 })
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            WolfWisdomTheme {
                LaunchedEffect(state.complete) {
                    if (state.complete) {
                        if (!pin) setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                        finish()
                    }
                }
                ConfigurationContent(state, { finish() }, model::select, model::confirm, model::retry)
            }
        }
    }
}

data class WidgetConfigUi(
    val id: Int = -1, val pin: Boolean = false, val width: Float = 160f,
    val theme: WidgetTheme = WidgetTheme.DARK, val quote: WidgetQuote? = null,
    val quoteId: Long? = null, val allowUnsaved: Boolean = false, val loading: Boolean = true, val submitting: Boolean = false,
    val error: Int? = null, val complete: Boolean = false
)

/** Draft is saved across process death; submission survives rotation and is guarded in one ViewModel. */
class WidgetConfigViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(WidgetConfigUi())
    val state = _state.asStateFlow()
    private var initialized = false

    fun initialize(id: Int, pin: Boolean, requestedQuote: Long?) {
        if (initialized) {
            // Activity recreation after a language change retains this ViewModel.
            if (_state.value.quote != null) viewModelScope.launch {
                val draft = _state.value
                val localizedQuote = withContext(Dispatchers.IO) {
                    loadWidgetQuote(getApplication(), WidgetState(draft.quoteId, draft.theme,
                        allowUnsaved = draft.allowUnsaved), allowUnsaved = draft.pin)
                }
                _state.value = _state.value.copy(quote = localizedQuote)
            }
            return
        }
        initialized = true
        _state.value = _state.value.copy(id = id, pin = pin, width = if (pin) 160f else
            WidgetUpdates.referenceWidth(AppWidgetManager.getInstance(getApplication()), id))
        viewModelScope.launch {
            try {
                val state = withContext(Dispatchers.IO) {
                    val committed = WidgetStateStore(getApplication()).read(id)
                    val initial = if (saved.contains("quote")) WidgetState(saved.get<Long>("quote")?.takeIf { it > 0 },
                        allowUnsaved = saved["allow_unsaved"] ?: false)
                        else committed ?: WidgetStateStore.initial(requestedQuote)
                    initial.copy(theme = saved.get<String>("theme")?.let { name -> WidgetTheme.entries.find { it.name == name } }
                        ?: committed?.theme ?: WidgetTheme.DARK)
                }
                saved["quote"] = state.quoteId ?: -1L
                saved["allow_unsaved"] = state.allowUnsaved
                _state.value = _state.value.copy(theme = state.theme, quoteId = state.quoteId, allowUnsaved = state.allowUnsaved,
                    quote = withContext(Dispatchers.IO) { loadWidgetQuote(getApplication(), state, allowUnsaved = pin) }, loading = false)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: java.io.IOException) { _state.value = _state.value.copy(loading = false, error = R.string.widget_load_error) }
        }
    }

    fun retry() {
        val old = _state.value
        initialized = false
        _state.value = old.copy(error = null, loading = true)
        initialize(old.id, old.pin, old.quoteId)
    }

    fun select(theme: WidgetTheme) {
        if (_state.value.submitting) return
        saved["theme"] = theme.name
        _state.value = _state.value.copy(theme = theme)
    }

    fun confirm() {
        val draft = _state.value
        if (draft.submitting || draft.loading || draft.quote == null || draft.complete) return
        _state.value = draft.copy(submitting = true, error = null)
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val state = WidgetState(draft.quoteId, draft.theme, allowUnsaved = draft.allowUnsaved)
                if (draft.pin && draft.quoteId != null) {
                    val result = GlobalContext.get().get<QuoteRepository>()
                        .setSaved(draft.quoteId, true)
                    if (result is ru.missclick.wolfwisdom.domain.util.Result.Failure) throw java.io.IOException("Cannot save pinned quote")
                }
                val success = if (draft.pin) WidgetPin.request(context, state) else withContext(Dispatchers.IO) {
                    if (!WidgetStateStore(context).write(draft.id, state)) throw java.io.IOException("Widget commit failed")
                    WidgetUpdates.update(context, draft.id)
                }
                _state.value = _state.value.copy(submitting = false, complete = success,
                    error = if (success) null else R.string.widget_create_error)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: java.io.IOException) {
                _state.value = _state.value.copy(submitting = false, error = R.string.widget_save_error)
            }
        }
    }
}

@Composable
private fun ConfigurationContent(state: WidgetConfigUi, back: () -> Unit, select: (WidgetTheme) -> Unit, confirm: () -> Unit, retry: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.fillMaxHeight().widthIn(max = 520.dp).fillMaxWidth().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.navigate_back)) }
                Text(stringResource(R.string.widget_configuration_title), fontFamily = Tinos, fontWeight = FontWeight.Normal, fontSize = 24.sp)
            }
            Spacer(Modifier.height(68.dp))
            Text(stringResource(R.string.widget_preview, when (state.width) { 248f -> "3×2"; 336f -> "4×2"; else -> "2×2" }),
                style = WolfTypography.Caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val width = minOf(state.width.dp, maxWidth)
                WidgetPreview(state, width.value)
            }
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.widget_theme_title), fontFamily = Tinos, fontWeight = FontWeight.Normal, fontSize = 22.sp)
            Spacer(Modifier.height(16.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 300.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.5f) {
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        WidgetTheme.entries.forEach { ThemeCard(it, state, select, Modifier.fillMaxWidth()) }
                    }
                } else Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WidgetTheme.entries.forEach { ThemeCard(it, state, select, Modifier.weight(1f)) }
                }
            }
            Spacer(Modifier.height(24.dp))
            state.error?.let {
                Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = WolfTypography.Body)
                if (state.quote == null) TextButton(onClick = retry) { Text(stringResource(R.string.widget_retry)) }
            }
            Button(onClick = confirm, enabled = !state.loading && !state.submitting && state.quote != null,
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(if (state.submitting) stringResource(R.string.widget_creating) else stringResource(R.string.widget_create), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun ThemeCard(theme: WidgetTheme, state: WidgetConfigUi, select: (WidgetTheme) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val palette = WidgetRenderer.palette(context, theme)
    val selected = theme == state.theme
    Surface(modifier = modifier.selectable(selected, enabled = !state.submitting && !state.loading, role = Role.RadioButton, onClick = { select(theme) }),
        shape = RoundedCornerShape(16.dp), color = Color(palette.surface),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else WolfColors.GlassBorder)) {
        Column(Modifier.padding(12.dp).heightIn(min = 100.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.widget_type_sample), fontFamily = Tinos, fontSize = 24.sp, color = Color(palette.text))
                if (selected) Icon(Icons.Default.Check, stringResource(R.string.widget_selected), tint = Color(palette.text), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(theme.labelRes), fontFamily = Inter, fontSize = 13.sp, color = Color(palette.text))
        }
    }
}

@Composable
private fun WidgetPreview(state: WidgetConfigUi, width: Float) {
    val context = LocalContext.current
    val quote = state.quote
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val view by produceState<RemoteViews?>(null, quote, state.theme, width, configuration) {
        value = if (quote == null) null else withContext(Dispatchers.IO) {
            WidgetRenderer.render(context, state.id, WidgetState(state.quoteId, state.theme), quote,
                WidgetDimensions(width, 160f), WidgetRenderer.budget(context))
        }
    }
    if (view == null) Box(Modifier.size(width.dp, 160.dp), contentAlignment = Alignment.Center) {
        if (state.loading || quote != null) CircularProgressIndicator()
    } else AndroidView(factory = { FrameLayout(it) }, modifier = Modifier.size(width.dp, 160.dp), update = { frame ->
        frame.removeAllViews()
        frame.addView(view!!.apply(context, frame))
    })
}

object WidgetPin {
    fun open(context: Context, quoteId: Long?) {
        if (!AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported) {
            Toast.makeText(context, R.string.widget_pin_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        context.startActivity(Intent(context, WidgetConfigurationActivity::class.java)
            .putExtra("request_pin", true).putExtra("quote_id", quoteId ?: -1L))
    }

    suspend fun request(context: Context, state: WidgetState): Boolean = withContext(Dispatchers.IO) {
        val token = java.util.UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences("widget_pin_requests", 0)
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("time.") && prefs.getLong(it, 0) < System.currentTimeMillis() - 86_400_000L }.forEach {
            editor.remove(it).remove(it.removePrefix("time."))
        }
        if (!editor.putString(token, "${state.quoteId ?: -1}|${state.theme.name}|${state.allowUnsaved}")
                .putLong("time.$token", System.currentTimeMillis()).commit()) throw java.io.IOException("Pin draft failed")
        val intent = Intent(context, WidgetPinReceiver::class.java).setAction("ru.missclick.wolfwisdom.PIN_WIDGET")
            .setData(android.net.Uri.parse("wolfwisdom://pin/$token"))
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (android.os.Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
        val callback = PendingIntent.getBroadcast(context, token.hashCode(), intent, flags)
        val success = try {
            AppWidgetManager.getInstance(context).requestPinAppWidget(ComponentName(context, FavoriteWidget2::class.java), null, callback)
        } catch (_: IllegalStateException) { false // Activity went into the background or user locked the device.
        } catch (_: SecurityException) { false }
        if (!success) prefs.edit().remove(token).remove("time.$token").commit()
        success
    }
}

class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.data?.lastPathSegment ?: return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (id < 0 || AppWidgetManager.getInstance(context).getAppWidgetInfo(id)?.provider != ComponentName(context, FavoriteWidget2::class.java)) return
        val pending = goAsync()
        WidgetUpdates.scope.launch {
            try {
                val prefs = context.getSharedPreferences("widget_pin_requests", 0)
                val fields = prefs.getString(token, null)?.split('|') ?: return@launch
                val theme = WidgetTheme.entries.find { it.name == fields.getOrNull(1) } ?: return@launch
                val allowUnsaved = fields.getOrNull(2)?.toBooleanStrictOrNull() ?: false
                if (WidgetStateStore(context).write(id, WidgetState(fields[0].toLongOrNull()?.takeIf { it > 0 }, theme, allowUnsaved))) {
                    prefs.edit().remove(token).remove("time.$token").commit()
                    WidgetUpdates.update(context, id)
                }
            } finally { pending.finish() }
        }
    }
}
