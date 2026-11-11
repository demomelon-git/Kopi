package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.KopiExtension
import com.example.ui.theme.ChromeBlue
import com.example.ui.theme.KopiCyan
import com.example.ui.theme.KopiGreen
import com.example.ui.theme.KopiPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    extensions: List<KopiExtension>,
    onToggleExtension: (KopiExtension) -> Unit,
    onInstallExtension: (KopiExtension) -> Unit,
    onInstallCrxBytes: (ByteArray, String) -> Unit,
    onUpdatePermissions: (KopiExtension, String) -> Unit,
    onDeleteExtension: (String) -> Unit,
    onOpenKubikStore: () -> Unit,
    onTriggerAction: (KopiExtension) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var permissionsExtension by remember { mutableStateOf<KopiExtension?>(null) }

    // File picker launcher for .crx files
    val crxFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val fileName = uri.lastPathSegment ?: "extension.crx"
                    onInstallCrxBytes(bytes, fileName)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка чтения CRX: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Расширения",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "kopi://extensions • Поддержка CRX и Kubik Store",
                            fontSize = 12.sp,
                            color = KopiPurple
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { crxFilePicker.launch("*/*") },
                        modifier = Modifier.testTag("install_crx_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Установить .CRX файл"
                        )
                    }
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_custom_extension_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить расширение"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Kubik Web Store Primary Integration Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = KopiPurple.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(KopiPurple.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = null,
                                    tint = KopiPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Kubik Web Store",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "demomelon-git.github.io/kubik-web-store/",
                                    fontSize = 11.sp,
                                    color = KopiCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Официальный магазин расширений: прямая установка .CRX пакетов с проверкой прав, кнопками действий и изолированным выполнением.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ElevatedButton(
                                onClick = onOpenKubikStore,
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = KopiPurple,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_kubik_store_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Kubik Store", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }

                            ElevatedButton(
                                onClick = { crxFilePicker.launch("*/*") },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = ChromeBlue,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("install_crx_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Из .CRX", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Установленные (${extensions.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { crxFilePicker.launch("*/*") },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Файл .CRX", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Скрипт", fontSize = 11.sp)
                        }
                    }
                }
            }

            items(extensions, key = { it.id }) { ext ->
                ExtensionCard(
                    extension = ext,
                    onToggle = { onToggleExtension(ext) },
                    onDelete = { onDeleteExtension(ext.id) },
                    onOpenPermissions = { permissionsExtension = ext },
                    onTriggerAction = { onTriggerAction(ext) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showAddDialog) {
        AddExtensionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { newExt ->
                onInstallExtension(newExt)
                showAddDialog = false
            }
        )
    }

    permissionsExtension?.let { ext ->
        ExtensionPermissionsDialog(
            extension = ext,
            onSavePermissions = { updatedPerms ->
                onUpdatePermissions(ext, updatedPerms)
                permissionsExtension = null
                Toast.makeText(context, "Разрешения для '${ext.name}' обновлены!", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { permissionsExtension = null }
        )
    }
}

@Composable
private fun ExtensionCard(
    extension: KopiExtension,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onOpenPermissions: () -> Unit,
    onTriggerAction: () -> Unit
) {
    var expandedCode by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KopiPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = KopiPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = extension.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ChromeBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = extension.source,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ChromeBlue
                            )
                        }
                    }
                    Text(
                        text = "v${extension.version} • ${extension.author}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = extension.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ChromeBlue,
                        checkedTrackColor = ChromeBlue.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("toggle_ext_${extension.id}")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = extension.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Run Action, Permissions Button, Code, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Кнопка действия расширения
                    if (extension.hasActionButton && extension.isEnabled) {
                        Button(
                            onClick = onTriggerAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChromeBlue,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("action_btn_${extension.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Действие", fontSize = 11.sp)
                        }
                    }

                    // Кнопка разрешения
                    OutlinedButton(
                        onClick = onOpenPermissions,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("permissions_btn_${extension.id}")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = KopiGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Кнопка разрешения", fontSize = 11.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { expandedCode = !expandedCode }) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (expandedCode) "Скрыть" else "Код", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expandedCode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(10.dp)
                ) {
                    Text(
                        text = extension.scriptContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ExtensionPermissionsDialog(
    extension: KopiExtension,
    onSavePermissions: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val rawPerms = extension.permissions.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
    val permStates = remember(extension.id) {
        mutableStateMapOf<String, Boolean>().apply {
            rawPerms.forEach { this[it] = true }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ChromeBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Кнопка разрешения", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(extension.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Управление правами доступа из manifest.json (${extension.source}):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (rawPerms.isEmpty()) {
                    Text(
                        text = "Специальных системных разрешений не требуется.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(rawPerms) { perm ->
                            val isAllowed = permStates[perm] ?: true
                            val explanation = when {
                                perm.contains("google.com") -> "Доступ к выдаче поиска Google"
                                perm.contains("bing.com") -> "Доступ к выдаче поиска Bing"
                                perm.contains("brave.com") -> "Доступ к выдаче поиска Brave"
                                perm.contains("duckduckgo.com") -> "Доступ к выдаче поиска DuckDuckGo"
                                perm.contains("startpage.com") -> "Доступ к поиску Startpage"
                                perm.contains("youtube.com") -> "Доступ к веб-плееру YouTube"
                                perm.contains("127.0.0.1") -> "Локальный интерфейс Кубика"
                                perm.contains("github.com") -> "Доступ к GitHub API"
                                perm == "activeTab" -> "Доступ к активной вкладке браузера"
                                perm == "storage" -> "Хранение настроек расширения"
                                perm == "declarativeNetRequest" -> "Правила сетевых запросов"
                                perm == "cookies" -> "Интеграция с контейнером Дата Копи"
                                perm == "tabs" -> "Чтение открытых вкладок"
                                perm == "scripting" -> "Выполнение скриптов на сайтах"
                                perm in listOf("<all_urls>", "*", "*://*/*") -> "Доступ ко всем сайтам в сети"
                                else -> "Право доступа: $perm"
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAllowed) KopiGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = perm,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllowed) KopiGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = explanation,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isAllowed,
                                        onCheckedChange = { permStates[perm] = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = KopiGreen,
                                            checkedTrackColor = KopiGreen.copy(alpha = 0.4f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        rawPerms.forEach { permStates[it] = true }
                    }) {
                        Text("Разрешить все", fontSize = 12.sp, color = KopiGreen)
                    }

                    TextButton(onClick = {
                        rawPerms.forEach { permStates[it] = false }
                    }) {
                        Text("Снять все", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val granted = rawPerms.filter { permStates[it] == true }.joinToString(", ")
                    onSavePermissions(granted)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChromeBlue)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Сохранить разрешения")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}

@Composable
fun InstallExtensionPromptDialog(
    extension: KopiExtension,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val perms = extension.permissions.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KopiPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = KopiPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Установить расширение?", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Kopi Browser • Chromium", fontSize = 11.sp, color = KopiPurple)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = extension.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Версия ${extension.version} • ${extension.source}",
                            fontSize = 11.sp,
                            color = ChromeBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = extension.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Запрашиваемые разрешения:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (perms.isEmpty()) {
                    Text(
                        text = "• Не требует специальных разрешений",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(perms) { perm ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = KopiGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = perm, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ChromeBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Разрешить и установить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun AddExtensionDialog(
    onDismiss: () -> Unit,
    onConfirm: (KopiExtension) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("Пользователь") }
    var description by remember { mutableStateOf("") }
    var matches by remember { mutableStateOf("*") }
    var script by remember {
        mutableStateOf("console.log('[Kopi Extension] Script loaded!');")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое расширение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название расширения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = matches,
                    onValueChange = { matches = it },
                    label = { Text("Фильтр сайтов (* для всех)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = script,
                    onValueChange = { script = it },
                    label = { Text("JavaScript инжект-код") },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && script.isNotBlank()) {
                        val newExt = KopiExtension(
                            id = "custom_" + System.currentTimeMillis(),
                            name = name,
                            version = "1.0.0",
                            description = description.ifBlank { "Пользовательский UserScript" },
                            author = author,
                            isEnabled = true,
                            matches = matches.ifBlank { "*" },
                            scriptContent = script,
                            source = "User Script",
                            permissions = "activeTab, storage",
                            hasActionButton = true,
                            actionTitle = name
                        )
                        onConfirm(newExt)
                    }
                },
                enabled = name.isNotBlank() && script.isNotBlank()
            ) {
                Text("Установить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
