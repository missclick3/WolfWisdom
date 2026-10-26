package ru.missclick.wolfwisdom.presentation.error

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = ViewModelStore()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun retry_repeatedTapWhileRunning_emitsOnlyOneRequest() = runTest(dispatcher) {
        val viewModel = ErrorViewModel().also { store.put("error", it) }
        viewModel.onAction(ErrorAction.Retry)
        viewModel.onAction(ErrorAction.Retry)

        assertTrue(viewModel.state.value.isRetrying)
        viewModel.retryRequests.first()
        var duplicateReceived = false
        val observer = backgroundScope.launch(dispatcher) {
            viewModel.retryRequests.first()
            duplicateReceived = true
        }
        assertFalse(duplicateReceived)
        observer.cancel()
    }

    @Test
    fun retry_afterPreviousAttemptFinishes_allowsAnotherRequest() = runTest(dispatcher) {
        val viewModel = ErrorViewModel().also { store.put("error", it) }
        viewModel.onAction(ErrorAction.Retry)
        viewModel.retryRequests.first()

        viewModel.onRetryFinished()
        assertFalse(viewModel.state.value.isRetrying)

        viewModel.onAction(ErrorAction.Retry)
        viewModel.retryRequests.first()
        assertTrue(viewModel.state.value.isRetrying)
    }
}
