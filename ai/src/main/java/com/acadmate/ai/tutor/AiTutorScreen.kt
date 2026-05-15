package com.acadmate.ai.tutor

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTutorScreen(
    mode: String = "TUTOR",
    initialSubject: String? = null,
    viewModel: AiTutorViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val realSyllabus by viewModel.realSyllabus.collectAsState()
    
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf<String?>(initialSubject) }
    var selectedUnit by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mode) {
        viewModel.setMode(mode)
    }
    
    val currentSyllabusList = if (realSyllabus.isNotEmpty()) realSyllabus 
                             else com.acadmate.core.model.PredefinedSyllabus.bTechCse6thSem

    val subjects = remember(currentSyllabusList) { 
        currentSyllabusList.map { it.subjectName } 
    }

    val units = remember(selectedSubject, currentSyllabusList) {
        if (selectedSubject == null) emptyList()
        else currentSyllabusList
            .find { it.subjectName == selectedSubject }
            ?.units?.map { it.title } ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when(mode) {
                            "INTERVIEW" -> "Interview Prep"
                            "PLANNER" -> "Study Planner"
                            else -> "AI Tutor"
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Subject Selector
            ScrollableTabRow(
                selectedTabIndex = subjects.indexOf(selectedSubject).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                subjects.forEach { subject ->
                    FilterChip(
                        selected = selectedSubject == subject,
                        onClick = { 
                            selectedSubject = if (selectedSubject == subject) null else subject 
                            selectedUnit = null
                        },
                        label = { Text(subject) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Unit Selector (Visible only if subject is selected)
            AnimatedVisibility(visible = units.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = units.indexOf(selectedUnit).coerceAtLeast(0),
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    units.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = if (selectedUnit == unit) null else unit },
                            label = { Text(unit) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }

            // Chat Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = true,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isTyping && messages.none { it.isStreaming }) {
                    item { TypingIndicator() }
                }
                
                items(messages.reversed(), key = { it.id }) { message ->
                    ChatBubble(message)
                }
            }

            // Suggestions
            AnimatedVisibility(
                visible = suggestions.isNotEmpty(),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { 
                                inputText = suggestion
                                viewModel.sendMessage(suggestion, selectedSubject, selectedUnit)
                            },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }

            // Input Area
            ChatInput(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText, selectedSubject, selectedUnit)
                    inputText = ""
                }
            )
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    
    val shape = if (isUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = backgroundColor,
                shape = shape,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (isUser) {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 15.sp
                        )
                    } else {
                        MarkdownText(
                            markdown = message.text,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                            fontSize = 10.sp,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        if (isUser) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlock(text: String) {
    // Simple code block rendering logic
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text.replace("```", ""),
            color = Color(0xFFD4D4D4),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition()
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { /* PDF Attachment Feature Coming Soon */ }) {
                Icon(Icons.Default.Add, contentDescription = "Attach")
            }
            
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask anything...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank(),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (value.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
        }
    }
}
