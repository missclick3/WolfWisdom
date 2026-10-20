package ru.missclick.wolfwisdom.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.missclick.wolfwisdom.presentation.components.WolfHeader
import ru.missclick.wolfwisdom.presentation.components.WolfMenuAction
import ru.missclick.wolfwisdom.presentation.components.WolfWisdomScreen
import ru.missclick.wolfwisdom.ui.theme.WolfWisdomTheme

@Composable
fun MainRoot(
    viewModel: MainViewModel = viewModel(),
    onMenuItemClick: (WolfMenuAction) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MainScreen(
        state = state,
        onAction = viewModel::onAction,
        onMenuItemClick = onMenuItemClick
    )
}

@Composable
fun MainScreen(
    state: MainState,
    onAction: (MainAction) -> Unit,
    onMenuItemClick: (WolfMenuAction) -> Unit = {},
) {
    WolfWisdomScreen {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                ,
                contentAlignment = Alignment.TopStart
            ) {
                WolfHeader(
                    isOptionsVisible = state.isOptionsVisible,
                    onOptionsClick = { onAction(MainAction.ToggleOptions) },
                    onOptionsDismissRequest = { onAction(MainAction.DismissOptions) },
                    onMenuItemClick = onMenuItemClick
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WolfWisdomTheme {
        MainScreen(
            state = MainState(),
            onAction = {}
        )
    }
}
