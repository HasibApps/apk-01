package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class BottomMenu { HOME, PROGRESS, DOWNLOADS, BACKUP, SETTINGS }

private data class BottomItem(val menu: BottomMenu, val label: String, val icon: ImageVector)

/**
 * অ্যাপের সব মেনু বাটন নিচের নেভিগেশন বারে (হোম / প্রোগ্রেস / ডাউনলোড / ব্যাকআপ / সেটিংস)।
 * নির্বাচিত আইটেমে ক্যাপসুল হাইলাইট।
 */
@Composable
fun AppBottomBar(
    selected: BottomMenu,
    onSelect: (BottomMenu) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomItem(BottomMenu.HOME, "হোম", Icons.Outlined.Home),
        BottomItem(BottomMenu.PROGRESS, "প্রোগ্রেস", Icons.Outlined.BarChart),
        BottomItem(BottomMenu.DOWNLOADS, "ডাউনলোড", Icons.Outlined.Download),
        BottomItem(BottomMenu.BACKUP, "ব্যাকআপ", Icons.Outlined.CloudUpload),
        BottomItem(BottomMenu.SETTINGS, "সেটিংস", Icons.Outlined.Settings)
    )
    val accent = Color(0xFF3B9CFF)
    val pill = accent.copy(alpha = 0.18f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSel = item.menu == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(item.menu) }
                        .padding(vertical = 2.dp)
                        .testTag("bottom_" + item.menu.name.lowercase())
                ) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) pill else Color.Transparent)
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSel) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
