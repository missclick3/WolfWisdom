package ru.missclick.wolfwisdom.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.withTimeoutOrNull
import ru.missclick.wolfwisdom.R
import ru.missclick.wolfwisdom.ui.theme.WolfColors
import ru.missclick.wolfwisdom.ui.theme.WolfRadii
import ru.missclick.wolfwisdom.ui.theme.WolfSpacing
import ru.missclick.wolfwisdom.ui.theme.WolfTypography
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme
import kotlin.time.Duration.Companion.milliseconds

private data class WolfSnackbarVisuals(
    override val message: String,
    val detail: String?,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
) : SnackbarVisuals

/** Cancelling the caller also removes its snackbar from the host. */
suspend fun SnackbarHostState.showWolfSnackbar(
    headline: String,
    detail: String? = null,
    durationMillis: Long = 2_000L
) {
    require(durationMillis > 0)
    withTimeoutOrNull(durationMillis.milliseconds) {
        showSnackbar(WolfSnackbarVisuals(message = headline, detail = detail))
    }
}

@Composable
fun WolfSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        WolfSnackbar(
            headline = data.visuals.message,
            detail = (data.visuals as? WolfSnackbarVisuals)?.detail,
            modifier = Modifier.padding(WolfSpacing.Space16)
        )
    }
}

@Composable
fun WolfSnackbar(
    headline: String,
    modifier: Modifier = Modifier,
    detail: String? = null
) {
    Surface(
        modifier = modifier.widthIn(max = 480.dp).fillMaxWidth(),
        shape = RoundedCornerShape(WolfRadii.Medium),
        color = WolfColors.SnackbarSurface,
        shadowElevation = WolfSpacing.Space16
    ) {
        BoxWithConstraints(
            modifier = Modifier.heightIn(min = 76.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                painter = painterResource(R.drawable.snackbar_wolf_forest),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(
                    start = maxWidth / 3,
                    end = WolfSpacing.Space16,
                    top = WolfSpacing.Space12,
                    bottom = WolfSpacing.Space12
                )
            ) {
                val textStyle = WolfTypography.Caption.copy(fontSize = 12.sp)
                Text(
                    text = headline,
                    style = textStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = WolfColors.Text
                )
                if (detail != null) {
                    Text(text = detail, style = textStyle, color = WolfColors.Secondary)
                }
            }
        }
    }
}

@Preview(widthDp = 390, locale = "ru")
@Composable
private fun WolfSnackbarPreview() {
    WolfWisdomTheme {
        WolfSnackbar(
            headline = "Нажмите ещё раз назад,",
            detail = "чтобы выйти из приложения",
            modifier = Modifier.padding(WolfSpacing.Space16)
        )
    }
}
