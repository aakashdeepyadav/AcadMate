package com.acadmate.dashboard.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.acadmate.designsystem.components.AcadMateCard
import com.acadmate.designsystem.components.CardVariant
import com.acadmate.designsystem.theme.SoftBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalLibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val books by viewModel.books.collectAsState()
    val myBooks by viewModel.myBooks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Digital Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Search") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("My Books") })
            }

            if (selectedTab == 0) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by title, author, or ISBN") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text("Available Textbooks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    val filteredBooks = books.filter { it.title.contains(searchQuery, true) || it.author.contains(searchQuery, true) }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredBooks) { book ->
                            BookItem(book)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("Currently Issued", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    if (myBooks.isEmpty()) {
                        item { Text("No books currently issued", color = Color.Gray) }
                    }
                    items(myBooks) { book ->
                        AcadMateCard(variant = CardVariant.Elevated) {
                            ListItem(
                                headlineContent = { Text(book.title) },
                                supportingContent = { Text("Due by: ${book.dueDate ?: "N/A"}") },
                                trailingContent = { Text("Issued", color = SoftBlue) },
                                leadingContent = { Icon(Icons.Default.Book, null, tint = SoftBlue) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookItem(book: Book) {
    AcadMateCard(variant = CardVariant.Flat) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Book, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.Bold)
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                book.status,
                style = MaterialTheme.typography.labelSmall,
                color = if (book.status == "Available") Color(0xFF10B981) else Color.Red
            )
        }
    }
}
