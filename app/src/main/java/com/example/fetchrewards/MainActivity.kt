package com.example.fetchrewards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.fetchrewards.data.model.Item
import com.example.fetchrewards.ui.viewmodel.ItemViewModel

class MainActivity : ComponentActivity() {

    private val itemViewModel: ItemViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FetchRewardsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ItemsScreen(viewModel = itemViewModel)
                }
            }
        }
    }
}

@Composable
fun ItemsScreen(
    viewModel: ItemViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is ItemViewModel.ItemUiState.Success -> ItemsList(items = state.groupedItems)
            is ItemViewModel.ItemUiState.Loading -> LoadingIndicator()
            is ItemViewModel.ItemUiState.Error -> ErrorMessage(
                error = state.error,
                onRetry = viewModel::fetchItems
            )
        }
    }
}

@Composable
fun ErrorMessage(
    error: ItemViewModel.ItemError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errorMessage = when (error) {
        is ItemViewModel.ItemError.EmptyList -> stringResource(R.string.error_no_items)
        is ItemViewModel.ItemError.NoValidItems -> stringResource(R.string.error_no_valid_items)
        is ItemViewModel.ItemError.Network -> stringResource(R.string.error_network)
        is ItemViewModel.ItemError.NotFound -> stringResource(R.string.error_not_found)
        is ItemViewModel.ItemError.Server -> stringResource(R.string.error_server)
        is ItemViewModel.ItemError.Http -> stringResource(R.string.error_http, error.code)
        is ItemViewModel.ItemError.ProcessingError -> stringResource(
            R.string.error_processing_items,
            error.exception.message ?: stringResource(R.string.error_unknown)
        )
        is ItemViewModel.ItemError.Unexpected -> stringResource(
            R.string.error_unexpected,
            error.exception.message ?: stringResource(R.string.error_unknown)
        )
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}


@Composable
fun ItemsList(
    items: Map<Int, List<Item>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items.toList()) { (listId, groupItems) ->
            ItemGroup(listId = listId, items = groupItems)
        }
    }
}

@Composable
fun ItemGroup(
    listId: Int,
    items: List<Item>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "$listId",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            items.forEach { item ->
                Text(
                    text = item.name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = onRetry) {
            Text(stringResource(id = R.string.retry))
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}


@Composable
fun FetchRewardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}