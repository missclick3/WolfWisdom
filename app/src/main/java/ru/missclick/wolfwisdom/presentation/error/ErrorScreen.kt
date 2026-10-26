package ru.missclick.wolfwisdom.presentation.error

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@Composable
fun ErrorRoot(
    viewModel: ErrorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ErrorScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun ErrorScreen(
    state: ErrorState,
    onAction: (ErrorAction) -> Unit,
) {

}

@Preview
@Composable
private fun Preview() {
    WolfWisdomTheme {
        ErrorScreen(
            state = ErrorState(),
            onAction = {}
        )
    }
}