package ru.missclick.wolfwisdom.presentation.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.missclick.wolfwisdom.BuildConfig
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.analytics.rememberScreenAnalytics
import ru.missclick.wolfwisdom.presentation.components.WolfGlassCard
import ru.missclick.wolfwisdom.presentation.components.WolfServiceScreen
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfTypography

private sealed interface DocumentState {
    data object Loading : DocumentState
    data object Failed : DocumentState
    data class Ready(val paragraphs: List<String>) : DocumentState
}

@Composable
fun LegalDocumentRoot(document: AboutDocument, onBackClick: () -> Unit) {
    val analytics = rememberScreenAnalytics { AboutDocumentAnalytics(it, document) }
    LegalDocumentScreen(document) { analytics.backClicked(); onBackClick() }
}

@Composable
fun LegalDocumentScreen(document: AboutDocument, onBackClick: () -> Unit) {
    val context = LocalContext.current
    var attempt by remember(document) { mutableIntStateOf(0) }
    val state by produceState<DocumentState>(DocumentState.Loading, document, attempt) {
        value = DocumentState.Loading
        value = try {
            withContext(Dispatchers.IO) {
                val files = when (document) {
                    AboutDocument.LICENSES -> listOf("legal/licenses.txt", "legal/third_party_notices.txt",
                        "font_licenses/inter_OFL.txt", "font_licenses/tinos_OFL.txt", "font_licenses/cormorant_garamond_OFL.txt")
                    AboutDocument.PRIVACY_POLICY -> listOf("legal/privacy.txt")
                    AboutDocument.TERMS -> listOf("legal/terms.txt")
                }
                DocumentState.Ready(files.flatMap { file ->
                    context.assets.open(file).bufferedReader(Charsets.UTF_8).use { it.readText() }
                        .replace("\r\n", "\n").split(Regex("\n[ \\t]*\n"))
                        .map(String::trim).filter(String::isNotEmpty)
                })
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            DocumentState.Failed
        }
    }
    val russian = LocalConfiguration.current.locales[0].language == "ru"
    WolfServiceScreen(stringResource(document.title), onBackClick, expandTitle = true) { backdrop ->
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.TopCenter) {
            WolfGlassCard(backdrop, Modifier.widthIn(max = 720.dp).fillMaxWidth().fillMaxHeight()) {
                when (val content = state) {
                    DocumentState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = WolfColors.Accent)
                    DocumentState.Failed -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.legal_load_error), color = WolfColors.Text)
                        TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.legal_retry), color = WolfColors.Accent) }
                    }
                    is DocumentState.Ready -> key(document) {
                        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                Text(stringResource(R.string.legal_updated), style = WolfTypography.Caption, color = WolfColors.Secondary)
                            }
                            if (!russian) item {
                                Text(stringResource(R.string.legal_russian_note), style = WolfTypography.Caption, color = WolfColors.Secondary)
                            }
                            itemsIndexed(content.paragraphs) { _, paragraph ->
                                val isHeading = paragraph.startsWith("# ")
                                SelectionContainer {
                                    Text(linkedDocumentText(paragraph.removePrefix("# ")),
                                        modifier = if (isHeading) Modifier.semantics { heading() } else Modifier,
                                        style = if (isHeading) WolfTypography.Title else WolfTypography.Body,
                                        color = WolfColors.Text)
                                }
                            }
                            item {
                                TextButton(onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", BuildConfig.CONTACT_EMAIL, null)))
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(context, R.string.contacts_app_unavailable, Toast.LENGTH_SHORT).show()
                                    }
                                }) { Text(stringResource(R.string.legal_contact), color = WolfColors.Accent) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun linkedDocumentText(text: String) = buildAnnotatedString {
    val links = Regex("https?://[^\\s]+|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    var cursor = 0
    links.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val label = match.value.trimEnd('.', ',', ';', ')')
        val url = if (label.startsWith("http")) label else "mailto:$label"
        withLink(LinkAnnotation.Url(url, TextLinkStyles(style = SpanStyle(color = WolfColors.Accent)))) { append(label) }
        append(match.value.removePrefix(label))
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}
