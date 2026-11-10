package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ChromeBlue
import com.example.ui.theme.KopiCyan

@Composable
fun ChromeOmnibox(
    url: String,
    isLoading: Boolean,
    progress: Int,
    tabCount: Int,
    onUrlSubmitted: (String) -> Unit,
    onRefreshClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onTabSwitcherClicked: () -> Unit,
    onMenuClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var textState by remember(url) { mutableStateOf(url) }
    val focusManager = LocalFocusManager.current

    val animatedProgress by animateFloatAsState(
        targetValue = progress / 100f,
        label = "progress"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Address Bar Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Security Icon
                        val isSecure = url.startsWith("https://")
                        val isInternal = url.startsWith("kopi://")
                        val icon = when {
                            isInternal -> Icons.Default.Shield
                            isSecure -> Icons.Default.Lock
                            else -> Icons.Default.Public
                        }
                        val tint = when {
                            isInternal -> KopiCyan
                            isSecure -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = if (isSecure) "Безопасное соединение" else "Адрес",
                            modifier = Modifier.size(16.dp),
                            tint = tint
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // URL / Search Input
                        BasicTextField(
                            value = textState,
                            onValueChange = { textState = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("omnibox_input")
                                .onFocusChanged { focusState ->
                                    isFocused = focusState.isFocused
                                    if (focusState.isFocused && (textState == "kopi://newtab")) {
                                        textState = ""
                                    }
                                },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(ChromeBlue),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    focusManager.clearFocus()
                                    onUrlSubmitted(textState)
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (textState.isEmpty() && !isFocused) {
                                    Text(
                                        text = "Поиск или веб-адрес",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        // Clear Button or Refresh/Stop Button
                        if (isFocused && textState.isNotEmpty()) {
                            IconButton(
                                onClick = { textState = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (isLoading) onStopClicked() else onRefreshClicked()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                    contentDescription = if (isLoading) "Остановить" else "Обновить",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Chrome Tab Switcher Button with Count Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable(onClick = onTabSwitcherClicked)
                        .testTag("tab_switcher_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tabCount > 99) ":D" else tabCount.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 3 Dots Chrome Menu Button
                IconButton(
                    onClick = onMenuClicked,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("chrome_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню Chrome",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Material 3 Progress Bar under Omnibox
            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = ChromeBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
