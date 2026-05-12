package com.acadmate.ai.exam

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MockExamContainer(
    viewModel: MockExamViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeLeft by viewModel.timeLeft.collectAsState()

    when (val state = uiState) {
        is ExamUiState.Setup -> {
            ExamSetupScreen(viewModel, onBack)
        }
        is ExamUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ExamUiState.Ongoing -> {
            ExamScreen(viewModel, state.questions, state.currentIndex, timeLeft)
        }
        is ExamUiState.Finished -> {
            ResultScreen(
                result = state.result,
                onRetry = { viewModel.reset() },
                onHome = onBack
            )
        }
        is ExamUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.reset() }) {
                        Text("Go Back")
                    }
                }
            }
        }
    }
}
