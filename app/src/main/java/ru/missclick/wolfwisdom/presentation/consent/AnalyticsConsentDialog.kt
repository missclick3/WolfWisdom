package ru.missclick.wolfwisdom.presentation.consent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.analytics.AnalyticsConsent
import ru.missclick.wolfwisdom.analytics.AnalyticsConsentChoice
import ru.missclick.wolfwisdom.analytics.AnalyticsConsentController
import ru.missclick.wolfwisdom.presentation.components.WolfLogoBrand
import ru.missclick.wolfwisdom.ui.theme.*

@Composable
fun AnalyticsConsentHost(controller: AnalyticsConsentController = AnalyticsConsent.controller) {
    val choice by controller.choice.collectAsStateWithLifecycle()
    if (choice == AnalyticsConsentChoice.UNKNOWN) {
        AnalyticsConsentDialog(onChoose = controller::choose)
    }
}

@Composable
internal fun AnalyticsConsentDialog(onChoose: (Boolean) -> Unit) {
    var showDetails by rememberSaveable { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { onChoose(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()
            .testTag("analytics-consent-backdrop")
            .pointerInput(onChoose) { detectTapGestures { onChoose(false) } }
            .safeDrawingPadding().padding(24.dp),
            contentAlignment = Alignment.Center) {
            val availableHeight = maxHeight
            Surface(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                    .heightIn(max = availableHeight).testTag("analytics-consent-dialog")
                    .pointerInput(Unit) { detectTapGestures { /* Consume taps inside the card. */ } },
                shape = RoundedCornerShape(28.dp),
                color = WolfColors.Surface,
                contentColor = WolfColors.Text,
                border = BorderStroke(1.dp, WolfColors.GlassBorder),
                shadowElevation = 20.dp
            ) {
                Column(Modifier.background(Brush.verticalGradient(listOf(
                    WolfPalette.Night800, WolfPalette.Night900
                )))) {
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WolfLogoBrand(size = 64.dp, contentDescription = null)
                        Text(stringResource(R.string.analytics_consent_title),
                            style = WolfTypography.Title, textAlign = TextAlign.Center)
                        Text(stringResource(R.string.analytics_consent_description),
                            modifier = Modifier.fillMaxWidth(),
                            style = WolfTypography.Body, color = WolfColors.Secondary)
                        Text(stringResource(R.string.analytics_consent_private),
                            modifier = Modifier.fillMaxWidth(),
                            style = WolfTypography.Body, color = WolfColors.Secondary)
                        Text(stringResource(R.string.analytics_consent_optional),
                            modifier = Modifier.fillMaxWidth(),
                            style = WolfTypography.Label, color = WolfColors.Accent)
                        TextButton(onClick = { showDetails = !showDetails },
                            modifier = Modifier.testTag("analytics-consent-more"),
                            colors = ButtonDefaults.textButtonColors(contentColor = WolfColors.Accent)) {
                            Text(stringResource(if (showDetails) R.string.analytics_details_hide else R.string.analytics_details_show),
                                style = WolfTypography.Label)
                        }
                        if (showDetails) {
                            Text(stringResource(R.string.analytics_consent_details),
                                modifier = Modifier.fillMaxWidth().testTag("analytics-consent-details"),
                                style = WolfTypography.Body, color = WolfColors.Secondary)
                        }
                    }
                    // Keep both choices reachable in landscape and with large text.
                    Column(Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ConsentButton(R.string.analytics_consent_allow, "analytics-consent-allow") { onChoose(true) }
                        ConsentButton(R.string.analytics_consent_decline, "analytics-consent-decline") { onChoose(false) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentButton(label: Int, tag: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(tag),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, WolfColors.GlassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = WolfColors.Pressed, contentColor = WolfColors.Accent
        )
    ) { Text(stringResource(label), style = WolfTypography.Label, textAlign = TextAlign.Center) }
}

@Preview(name = "Consent · Phone", widthDp = 390, heightDp = 844, locale = "ru")
@Preview(name = "Consent · Landscape", widthDp = 844, heightDp = 390, locale = "ru")
@Preview(name = "Consent · Large text", widthDp = 390, heightDp = 844, fontScale = 1.5f, locale = "ru")
@Composable
private fun ConsentPreview() {
    WolfWisdomTheme { AnalyticsConsentDialog {} }
}
