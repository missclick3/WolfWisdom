package ru.missclick.wolfwisdom.presentation.error

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.presentation.util.DeviceConfiguration
import ru.missclick.wolfwisdom.presentation.util.currentDeviceConfiguration
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

/** The caller retries the failed operation and leaves this screen on success. */
@Composable
fun ErrorRoot(
    onRetry: suspend () -> Unit,
    viewModel: ErrorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val retry by rememberUpdatedState(onRetry)
    LaunchedEffect(viewModel) {
        viewModel.retryRequests.collect {
            try {
                retry()
            } finally {
                viewModel.onRetryFinished()
            }
        }
    }
    ErrorScreen(state = state, onAction = viewModel::onAction)
}

// Colors in the error frames differ slightly from the main screen's palette.
private val ErrorBackground = Color(0xFF061019)
private val ErrorText = Color(0xFFF1F4FA)
private val ErrorMuted = Color(0xFF8CA4BD)
private val ErrorSecondary = Color(0xFFA7BDD3)
private val RetryColor = Color(0xFFCCE0FF)

@Composable
fun ErrorScreen(
    state: ErrorState,
    onAction: (ErrorAction) -> Unit,
) {
    val device = currentDeviceConfiguration()
    val portraitPhone = device == DeviceConfiguration.MOBILE_PORTRAIT
    val landscapePhone = device == DeviceConfiguration.MOBILE_LANDSCAPE
    val contentStart = when (device) {
        DeviceConfiguration.MOBILE_PORTRAIT -> 0.409f
        DeviceConfiguration.MOBILE_LANDSCAPE -> 0.354f
        DeviceConfiguration.TABLET_PORTRAIT -> 0.433f
        DeviceConfiguration.TABLET_LANDSCAPE -> 0.404f
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(ErrorBackground).safeDrawingPadding()
    ) {
        val screenHeight = maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (portraitPhone) 20.dp else 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                Modifier.fillMaxWidth().padding(top = 28.dp),
                horizontalAlignment = if (portraitPhone) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    stringResource(R.string.error_brand_title),
                    style = WolfTypography.Title,
                    color = ErrorText
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.app_subtitle).uppercase(),
                    style = WolfTypography.Caption.copy(fontSize = 9.sp, lineHeight = 11.sp),
                    color = ErrorMuted
                )
            }
            // Anchor the message in the same part of the window as the adaptive Figma frames.
            // The scroll container keeps the action reachable with large accessibility fonts.
            Spacer(Modifier.height((screenHeight * contentStart - 71.dp).coerceAtLeast(24.dp)))
            Text(
                stringResource(R.string.error_load_label),
                style = WolfTypography.Caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = ErrorMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(if (portraitPhone) 20.dp else 12.dp))
            Text(
                stringResource(if (portraitPhone) R.string.error_title_compact else R.string.error_title),
                modifier = Modifier.widthIn(max = if (portraitPhone) 250.dp else 640.dp)
                    .semantics { heading() },
                style = WolfTypography.Display.copy(
                    fontSize = if (landscapePhone) 28.sp else if (portraitPhone) 32.sp else 36.sp,
                    lineHeight = if (landscapePhone) 36.sp else if (portraitPhone) 40.sp else 44.sp
                ),
                color = ErrorText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(if (portraitPhone) 20.dp else 8.dp))
            Text(
                stringResource(R.string.error_description),
                style = WolfTypography.Body.copy(
                    fontSize = if (landscapePhone) 14.sp else 16.sp,
                    lineHeight = if (portraitPhone) 20.sp else 24.sp
                ),
                color = ErrorSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(when {
                portraitPhone -> 40.dp
                landscapePhone -> 24.dp
                else -> 60.dp
            }))
            Button(
                onClick = { onAction(ErrorAction.Retry) },
                enabled = !state.isRetrying,
                modifier = Modifier.widthIn(max = when {
                    portraitPhone -> 350.dp
                    landscapePhone -> 260.dp
                    else -> 320.dp
                }).fillMaxWidth().heightIn(min = if (landscapePhone) 48.dp else 52.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RetryColor,
                    contentColor = ErrorBackground,
                    disabledContainerColor = RetryColor.copy(alpha = 0.6f),
                    disabledContentColor = ErrorBackground
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    stringResource(R.string.error_retry),
                    style = WolfTypography.Label.copy(fontSize = 16.sp, lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(name = "Phone", widthDp = 390, heightDp = 844, locale = "ru")
@Preview(name = "Phone landscape", widthDp = 844, heightDp = 390, locale = "ru")
@Preview(name = "Tablet", widthDp = 834, heightDp = 1194, locale = "ru")
@Preview(name = "Tablet landscape", widthDp = 1194, heightDp = 834, locale = "ru")
@Composable
private fun ErrorScreenPreview() {
    WolfWisdomTheme { ErrorScreen(state = ErrorState(), onAction = {}) }
}
