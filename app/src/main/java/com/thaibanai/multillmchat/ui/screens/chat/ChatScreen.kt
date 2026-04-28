package com.thaibanai.multillmchat.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.thaibanai.multillmchat.data.remote.model.LLMProvider
import com.thaibanai.multillmchat.domain.model.*
import com.thaibanai.multillmchat.ui.components.MarkdownView
import com.thaibanai.multillmchat.ui.theme.*
import com.thaibanai.multillmchat.util.TempFileUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String? = null,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToConversations: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize with conversation ID once
    LaunchedEffect(conversationId) {
        viewModel.initialize(conversationId)
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImageAttachment(it) }
    }

    // Document picker
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addDocumentAttachment(it) }
    }

    // Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val tempUri = TempFileUtil.saveBitmapToCache(context, it)
            tempUri?.let { uri -> viewModel.addImageAttachment(uri) }
        }
    }

    // Auto-scroll to bottom
    val messageCount = uiState.messages.size
    LaunchedEffect(messageCount) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    var showAttachmentSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.conversationTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ChatInputBar(
                inputText = uiState.inputText,
                onInputChange = { viewModel.updateInputText(it) },
                onSend = { viewModel.sendMessage() },
                selectedProviders = uiState.selectedProviders,
                providerStatuses = uiState.providerStatuses,
                onToggleProvider = { viewModel.toggleProvider(it) },
                onAttachClick = { showAttachmentSheet = true },
                pendingAttachments = uiState.pendingAttachments,
                onRemoveAttachment = { viewModel.removeAttachment(it) },
                isLoading = uiState.isLoading
            )
        }
    ) { padding ->
        if (uiState.messages.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Send a message to start",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Select one or more providers above\nthe input field",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        context = context,
                        onCopy = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
                        },
                        onDelete = { viewModel.deleteMessage(message.id) },
                        onResend = {
                            viewModel.resendToProviders(
                                message.content,
                                uiState.selectedProviders.toList(),
                                emptyList()
                            )
                        }
                    )
                }
            }
        }
    }

    // Attachment bottom sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Attach File",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                AttachmentOption(
                    icon = Icons.Filled.CameraAlt,
                    title = "Camera",
                    subtitle = "Take a photo",
                    onClick = {
                        showAttachmentSheet = false
                        cameraLauncher.launch(null)
                    }
                )
                AttachmentOption(
                    icon = Icons.Filled.Photo,
                    title = "Gallery",
                    subtitle = "Choose an image",
                    onClick = {
                        showAttachmentSheet = false
                        imagePickerLauncher.launch("image/*")
                    }
                )
                AttachmentOption(
                    icon = Icons.Filled.Description,
                    title = "Document",
                    subtitle = "PDF, TXT, or DOCX",
                    onClick = {
                        showAttachmentSheet = false
                        documentPickerLauncher.launch("*/*")
                    }
                )
            }
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChatInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    selectedProviders: Set<LLMProvider>,
    providerStatuses: List<ProviderStatus>,
    onToggleProvider: (LLMProvider) -> Unit,
    onAttachClick: () -> Unit,
    pendingAttachments: List<PendingAttachment>,
    onRemoveAttachment: (String) -> Unit,
    isLoading: Boolean
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            // Provider toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                providerStatuses.forEach { status ->
                    ProviderToggleChip(
                        provider = status.provider,
                        isSelected = selectedProviders.contains(status.provider),
                        isEnabled = status.hasApiKey,
                        onClick = { if (status.hasApiKey) onToggleProvider(status.provider) }
                    )
                }
            }

            // Pending attachments
            if (pendingAttachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    pendingAttachments.forEach { attachment ->
                        AttachmentChip(attachment = attachment, onRemove = { onRemoveAttachment(attachment.id) })
                    }
                }
            }

            // Input row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = onAttachClick) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Attach")
                }
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message…") },
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (inputText.isNotBlank()) onSend() }),
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(4.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = (inputText.isNotBlank() || pendingAttachments.isNotEmpty()) && !isLoading,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderToggleChip(
    provider: LLMProvider,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val color = when (provider) {
        LLMProvider.CLAUDE -> ClaudeOrange
        LLMProvider.OPENAI -> OpenAiGreen
        LLMProvider.DEEPSEEK -> DeepSeekBlue
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        enabled = isEnabled,
        label = { Text(provider.displayName, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.2f),
            selectedLeadingIconColor = color
        )
    )
}

@Composable
private fun AttachmentChip(attachment: PendingAttachment, onRemove: () -> Unit) {
    val icon = when (attachment.type) {
        AttachmentType.IMAGE -> Icons.Filled.Image
        AttachmentType.PDF -> Icons.Filled.PictureAsPdf
        AttachmentType.TXT -> Icons.Filled.TextSnippet
        AttachmentType.DOCX -> Icons.Filled.Description
    }

    AssistChip(
        onClick = {},
        label = { Text(attachment.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
            }
        },
        modifier = Modifier.height(32.dp)
    )
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    context: Context,
    onCopy: (String) -> Unit,
    onDelete: () -> Unit,
    onResend: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    if (message.role == MessageRole.USER) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            if (message.attachments.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(bottom = 4.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    message.attachments.forEach { attachment ->
                        if (attachment.type == AttachmentType.IMAGE && attachment.thumbnailUri != null) {
                            AsyncImage(
                                model = attachment.thumbnailUri,
                                contentDescription = "Attached image",
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.widthIn(max = 300.dp).padding(start = 48.dp, bottom = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = message.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    } else {
        val providerColor = when (message.provider) {
            LLMProvider.CLAUDE -> ClaudeOrange
            LLMProvider.OPENAI -> OpenAiGreen
            LLMProvider.DEEPSEEK -> DeepSeekBlue
            null -> MaterialTheme.colorScheme.primary
        }

        Column(modifier = Modifier.fillMaxWidth().padding(end = 48.dp, bottom = 8.dp)) {
            // Provider label
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(providerColor))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = message.provider?.displayName ?: "Assistant",
                    style = MaterialTheme.typography.labelMedium,
                    color = providerColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (message.model != null) {
                    Text(
                        text = " · ${message.model}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Message card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.widthIn(max = 300.dp).padding(bottom = 2.dp)
            ) {
                Column {
                    Box(modifier = Modifier.padding(12.dp).widthIn(max = 300.dp)) {
                        if (message.isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("thinking…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else if (message.error != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(text = message.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                        } else {
                            // Render markdown using MarkdownView
                            MarkdownView(
                                markdown = message.content,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        if (message.content.isNotBlank()) {
                            IconButton(onClick = { onCopy(message.content) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Copy") },
                                    onClick = { showMenu = false; onCopy(message.content) },
                                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = { showMenu = false; onDelete() },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Re-send") },
                                    onClick = { showMenu = false; onResend() },
                                    leadingIcon = { Icon(Icons.Outlined.Refresh, contentDescription = null) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
