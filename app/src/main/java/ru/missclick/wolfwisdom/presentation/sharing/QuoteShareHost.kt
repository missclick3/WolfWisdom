package ru.missclick.wolfwisdom.presentation.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.components.QuoteLandscapeCard
import ru.missclick.wolfwisdom.presentation.components.WolfSnackbarHost
import ru.missclick.wolfwisdom.presentation.components.showWolfSnackbar
import ru.missclick.wolfwisdom.ui.theme.WolfLayout
import kotlin.random.Random

private const val ExportWidthPx = 1080
private const val CardWidthDp = 378f
private const val ShareDirectory = "shared_quotes"

private data class QuoteShareRequest(val quote: String, val sceneIndex: Int)

/** One export/share pipeline for every screen. A null scene chooses random artwork. */
@Composable
internal fun QuoteShareHost(
    modifier: Modifier = Modifier,
    content: @Composable (shareQuote: (String, Int?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val shareError = stringResource(R.string.quote_share_error)
    var request by remember { mutableStateOf<QuoteShareRequest?>(null) }

    Box(modifier.fillMaxSize()) {
        content { quote, scene ->
            if (request == null && quote.isNotBlank()) {
                request = QuoteShareRequest(
                    quote,
                    scene ?: Random.nextInt(WolfLayout.SavedCardHeights.size)
                )
            }
        }
        request?.let { current ->
            QuoteCardCapture(current.quote, current.sceneIndex) { captured ->
                try {
                    val intent = createQuoteShareIntent(context, captured.getOrThrow())
                    context.startActivity(Intent.createChooser(intent, null))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Log.e("QuoteShare", "Unable to share quote card", error)
                    scope.launch {
                        snackbar.showWolfSnackbar(shareError)
                    }
                } finally {
                    request = null
                }
            }
        }
        WolfSnackbarHost(
            snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )
    }
}

/** Records the real card composable without painting it over the current screen. */
@Composable
internal fun QuoteCardCapture(
    quote: String,
    sceneIndex: Int,
    onCaptured: suspend (Result<ImageBitmap>) -> Unit
) {
    val layer = rememberGraphicsLayer()
    val recorded = remember(quote, sceneIndex) { CompletableDeferred<Unit>() }
    val artwork = ImageBitmap.imageResource(R.drawable.saved_landscapes)

    LaunchedEffect(recorded) {
        recorded.await()
        val result = try {
            Result.success(layer.toImageBitmap())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        onCaptured(result)
    }
    CompositionLocalProvider(LocalDensity provides Density(ExportWidthPx / CardWidthDp, 1f)) {
        Layout(
            modifier = Modifier.clearAndSetSemantics {},
            content = {
                QuoteLandscapeCard(
                    quote = quote,
                    artwork = artwork,
                    sceneIndex = sceneIndex,
                    showBorder = false,
                    modifier = Modifier.drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        recorded.complete(Unit)
                        // The recorded layer is exported, never drawn into the screen.
                    }
                )
            }
        ) { measurables, _ ->
            // Unbounded height keeps the entire quote, even beyond the viewport.
            val card = measurables.single().measure(
                Constraints(minWidth = ExportWidthPx, maxWidth = ExportWidthPx)
            )
            layout(1, 1) { card.place(0, 0) }
        }
    }
}

internal suspend fun createQuoteShareIntent(context: Context, image: ImageBitmap): Intent =
    withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, ShareDirectory).apply { mkdirs() }
        // Keep recent attachments alive for recipients reading them after the chooser closes.
        val expiry = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        directory.listFiles()?.filter { it.lastModified() < expiry }?.forEach { it.delete() }
        val file = File.createTempFile("wolf_quote_", ".png", directory)
        try {
            file.outputStream().use {
                check(image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.quote_shares", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, context.getString(R.string.share), uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }
