package com.sonus.player.ui.lyrics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.notAvailable -> {
                Text(
                    text = "Letras no disponibles",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            uiState.isSynced -> {
                SyncedLyricsView(
                    lines = uiState.lines.map { it.text },
                    currentLineIndex = uiState.currentLineIndex
                )
            }
            uiState.plainText != null -> {
                PlainLyricsView(text = uiState.plainText!!)
            }
        }

        // Source indicator
        if (uiState.source.isNotEmpty()) {
            Text(
                text = "Fuente: ${uiState.source}",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lines: List<String>,
    currentLineIndex: Int
) {
    val listState = rememberLazyListState()

    // Auto-scroll to current line
    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(
                index = currentLineIndex,
                scrollOffset = -200 // offset to center-ish
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        item { Spacer(modifier = Modifier.height(100.dp)) }

        itemsIndexed(lines) { index, line ->
            val isCurrentLine = index == currentLineIndex
            Text(
                text = line,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isCurrentLine) 20.sp else 16.sp,
                    fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isCurrentLine) {
                    Color(0xFFFFD700) // Gold highlight
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                textAlign = TextAlign.Center
            )
        }

        item { Spacer(modifier = Modifier.height(200.dp)) }
    }
}

@Composable
private fun PlainLyricsView(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 28.sp
        )
        Spacer(modifier = Modifier.height(100.dp))
    }
}
