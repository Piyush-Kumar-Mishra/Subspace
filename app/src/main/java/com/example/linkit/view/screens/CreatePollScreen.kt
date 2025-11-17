package com.example.linkit.view.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.linkit.viewmodel.PollViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollScreen(
    projectId: Long,
    viewModel: PollViewModel,
    onNavigateBack: () -> Unit
) {
    val pollState by viewModel.pollState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create / Replace Poll") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.createPoll(projectId) }) {
                Icon(Icons.Default.Send, contentDescription = "Create Poll")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = pollState.question,
                    onValueChange = { viewModel.onPollQuestionChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Poll Question") },
                    maxLines = 3
                )
            }

            itemsIndexed(pollState.options) { index, option ->
                OutlinedTextField(
                    value = option,
                    onValueChange = { viewModel.onPollOptionChanged(index, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Option ${index + 1}") },
                    trailingIcon = {
                        if (pollState.options.size > 2) {
                            IconButton(onClick = { viewModel.removePollOption(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Option")
                            }
                        }
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = { viewModel.addPollOption() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Option")
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Allow multiple answers")
                    Switch(
                        checked = pollState.allowMultipleAnswers,
                        onCheckedChange = { viewModel.onAllowMultipleAnswersChanged(it) }
                    )
                }
            }
        }
    }
}