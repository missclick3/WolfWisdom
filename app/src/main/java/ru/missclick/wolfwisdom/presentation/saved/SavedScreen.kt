package ru.missclick.wolfwisdom.presentation.saved

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.missclick.wolfwisdom.presentation.components.WolfWisdomScreen
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@Composable
fun SavedRoot(
    viewModel: SavedViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SavedScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SavedScreen(
    state: SavedState,
    onAction: (SavedAction) -> Unit,
) {
    WolfWisdomScreen {}
}

@Preview
@Composable
private fun Preview() {
    WolfWisdomTheme {
        SavedScreen(
            state = SavedState(),
            onAction = {}
        )
    }
}