package com.example.fetchrewards.data.repository.ItemViewModelTest

import app.cash.turbine.test
import com.example.fetchrewards.data.model.Item
import com.example.fetchrewards.data.repository.ItemRepository
import com.example.fetchrewards.ui.viewmodel.ItemViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import java.io.IOException
import retrofit2.Response
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class ItemViewModelTest {

    private lateinit var viewModel: ItemViewModel
    private val repository: ItemRepository = mock()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(testDispatcher)
        whenever(repository.getItems()).thenReturn(Result.success(emptyList()))
        viewModel = ItemViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel = ItemViewModel(repository)
        assertTrue(viewModel.uiState.value is ItemViewModel.ItemUiState.Loading)
    }

    @Test
    fun `fetchItems with empty list returns error state`() = runTest {
        // Given
        whenever(repository.getItems()).thenReturn(Result.success(emptyList()))

        viewModel.uiState.test(timeout = 5.seconds) {
            skipItems(1)

            // When
            viewModel.fetchItems()

            // Then
            assertEquals(ItemViewModel.ItemUiState.Loading, awaitItem())
            assertEquals(ItemViewModel.ItemUiState.Error(ItemViewModel.ItemError.EmptyList), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchItems success with valid items updates state correctly`() = runTest {
        // Given
        val items = listOf(
            Item(1, 1, "B Item"),
            Item(2, 1, "A Item"),
            Item(3, 2, "C Item")
        )
        whenever(repository.getItems()).thenReturn(Result.success(items))

        viewModel.uiState.test(timeout = 5.seconds) {
            skipItems(1)

            // When
            viewModel.fetchItems()

            // Then
            assertEquals(ItemViewModel.ItemUiState.Loading, awaitItem())
            val successState = awaitItem() as ItemViewModel.ItemUiState.Success
            val groupedItems = successState.groupedItems

            assertEquals(2, groupedItems.size)
            assertTrue(groupedItems.containsKey(1))
            assertTrue(groupedItems.containsKey(2))
            assertEquals("A Item", groupedItems[1]?.first()?.name)
            assertEquals("B Item", groupedItems[1]?.last()?.name)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchItems with all invalid items returns error state`() = runTest {
        // Given
        val items = listOf(
            Item(1, 1, ""),
            Item(2, 1, null),
            Item(3, 1, "  ")
        )
        whenever(repository.getItems()).thenReturn(Result.success(items))

        viewModel.uiState.test(timeout = 5.seconds) {
            skipItems(1)

            // When
            viewModel.fetchItems()

            // Then
            assertEquals(ItemViewModel.ItemUiState.Loading, awaitItem())
            assertEquals(ItemViewModel.ItemUiState.Error(ItemViewModel.ItemError.NoValidItems), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchItems with network error returns appropriate error state`() = runTest {
        // Given
        val exception = IOException("Network error")
        whenever(repository.getItems()).thenReturn(Result.failure(exception))

        viewModel.uiState.test(timeout = 5.seconds) {
            skipItems(1)

            // When
            viewModel.fetchItems()

            // Then
            assertEquals(ItemViewModel.ItemUiState.Loading, awaitItem())
            assertEquals(ItemViewModel.ItemUiState.Error(ItemViewModel.ItemError.Network), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchItems with HTTP 404 error returns not found error state`() = runTest {
        // Given
        val mockResponse: Response<List<Item>> = Response.error(
            404,
            okhttp3.ResponseBody.create(null, "")
        )
        whenever(repository.getItems()).thenReturn(
            Result.failure(HttpException(mockResponse))
        )

        viewModel.uiState.test(timeout = 5.seconds) {
            skipItems(1)

            // When
            viewModel.fetchItems()

            // Then
            assertEquals(ItemViewModel.ItemUiState.Loading, awaitItem())
            assertEquals(ItemViewModel.ItemUiState.Error(ItemViewModel.ItemError.NotFound), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchItems with HTTP 500 error returns server error state`() = runTest {
        // Given
        val mockResponse: Response<List<Item>> = Response.error(
            500,
            okhttp3.ResponseBody.create(null, "")
        )
        whenever(repository.getItems()).thenReturn(
            Result.failure(HttpException(mockResponse))
        )

        viewModel.uiState.test(timeout = 5.seconds) {
            skipItems(1)

            // When
            viewModel.fetchItems()

            // Then
            assertEquals(ItemViewModel.ItemUiState.Loading, awaitItem())
            assertEquals(ItemViewModel.ItemUiState.Error(ItemViewModel.ItemError.Server), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchItems with unknown HTTP error returns HTTP error state`() = runTest {
        // Given
        val mockResponse: Response<List<Item>> = Response.error(
            418,
            okhttp3.ResponseBody.create(null, "")
        )
        whenever(repository.getItems()).thenReturn(
            Result.failure(HttpException(mockResponse))
        )

        viewModel.uiState.test(timeout = 5.seconds) {
            skipItems(1)

            // When
            viewModel.fetchItems()

            // Then
            assertEquals(ItemViewModel.ItemUiState.Loading, awaitItem())
            assertEquals(ItemViewModel.ItemUiState.Error(ItemViewModel.ItemError.Http(418)), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `fetchItems with unexpected error returns unexpected error state`() = runTest {
        // Given
        val exception = Exception()
        whenever(repository.getItems()).thenReturn(Result.failure(exception))

        viewModel.uiState.test(timeout = 5.seconds) {
            skipItems(1)

            // When
            viewModel.fetchItems()

            // Then
            assertEquals(ItemViewModel.ItemUiState.Loading, awaitItem())
            assertEquals(ItemViewModel.ItemUiState.Error(ItemViewModel.ItemError.Unexpected(exception)), awaitItem())

            cancelAndConsumeRemainingEvents()
        }
    }
}