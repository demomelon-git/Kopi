package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.InternalScreen
import com.example.ui.theme.ChromeBlue
import com.example.ui.theme.KopiCyan

@Composable
fun ChromeMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isBookmarked: Boolean,
    desktopMode: Boolean,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onToggleBookmark: () -> Unit,
    onShare: () -> Unit,
    onNewTab: () -> Unit,
    onNewIncognitoTab: () -> Unit,
    onOpenScreen: (InternalScreen) -> Unit,
    onToggleDesktopMode: () -> Unit,
    onFindInPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .width(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top Icon Actions Row (Back, Forward, Bookmark, Share, Info)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onGoBack(); onDismissRequest() },
                enabled = canGoBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(
                onClick = { onGoForward(); onDismissRequest() },
                enabled = canGoForward,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Вперед",
                    tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(
                onClick = { onToggleBookmark(); onDismissRequest() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Закладка",
                    tint = if (isBookmarked) ChromeBlue else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = { onShare(); onDismissRequest() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Поделиться",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // New Tab
        MenuEntry(
            title = "Новая вкладка",
            icon = Icons.Default.Add,
            onClick = { onNewTab(); onDismissRequest() }
        )

        // New Incognito Tab
        MenuEntry(
            title = "Новая вкладка инкогнито",
            icon = Icons.Default.VisibilityOff,
            onClick = { onNewIncognitoTab(); onDismissRequest() }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Experimental internal links per user requirements!
        MenuEntry(
            title = "Расширения (kopi://extensions)",
            icon = Icons.Default.Extension,
            badge = "Kubik Store",
            onClick = { onOpenScreen(InternalScreen.Extensions); onDismissRequest() }
        )

        MenuEntry(
            title = "Флаги (kopi://flags)",
            icon = Icons.Default.Flag,
            badge = "Эксперименты",
            onClick = { onOpenScreen(InternalScreen.Flags); onDismissRequest() }
        )

        MenuEntry(
            title = "Веб-приложения (kopi://apps)",
            icon = Icons.Default.Apps,
            onClick = { onOpenScreen(InternalScreen.Apps); onDismissRequest() }
        )

        MenuEntry(
            title = "Дата Копи (kopi://datakopi)",
            icon = Icons.Default.VpnKey,
            badge = "Active",
            onClick = { onOpenScreen(InternalScreen.DataKopi); onDismissRequest() }
        )

        MenuEntry(
            title = "Синхронизация устройств",
            icon = Icons.Default.Sync,
            onClick = { onOpenScreen(InternalScreen.Sync); onDismissRequest() }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        MenuEntry(
            title = "Закладки",
            icon = Icons.Default.BookmarkBorder,
            onClick = { onOpenScreen(InternalScreen.Bookmarks); onDismissRequest() }
        )

        MenuEntry(
            title = "История",
            icon = Icons.Default.History,
            onClick = { onOpenScreen(InternalScreen.History); onDismissRequest() }
        )

        MenuEntry(
            title = "Скачанные файлы",
            icon = Icons.Default.Download,
            onClick = { onOpenScreen(InternalScreen.Downloads); onDismissRequest() }
        )

        MenuEntry(
            title = "Найти на странице",
            icon = Icons.Default.FindInPage,
            onClick = { onFindInPage(); onDismissRequest() }
        )

        // Desktop site toggle
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Версия для ПК",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            trailingIcon = {
                Checkbox(
                    checked = desktopMode,
                    onCheckedChange = { onToggleDesktopMode() }
                )
            },
            onClick = {
                onToggleDesktopMode()
            }
        )

        MenuEntry(
            title = "Настройки",
            icon = Icons.Default.Settings,
            onClick = { onOpenScreen(InternalScreen.Settings); onDismissRequest() }
        )
    }
}

@Composable
private fun MenuEntry(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(KopiCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = KopiCyan
                        )
                    }
                }
            }
        },
        onClick = onClick
    )
}
