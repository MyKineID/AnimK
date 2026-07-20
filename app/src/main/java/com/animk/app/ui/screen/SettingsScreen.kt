package com.animk.app.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animk.app.ui.theme.LocalCustomColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val custom = LocalCustomColors.current
    val context = LocalContext.current

    var cellularDataMode by remember { mutableStateOf("Automatic") }
    var downloadWifiOnly by remember { mutableStateOf(true) }
    var downloadQuality by remember { mutableStateOf("High (1080p)") }
    var notifyNewReleases by remember { mutableStateOf(true) }
    var notifyRecommendations by remember { mutableStateOf(false) }

    var cacheSizeMb by remember { mutableIntStateOf(148) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDataUsageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "App Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = custom.textPrimary,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Profile Header Card (User specified Profile name: AnimK)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = custom.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(custom.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = custom.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AnimK",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = custom.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Premium 4K + HDR Plan",
                        fontSize = 12.sp,
                        color = custom.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    color = custom.cardSurface,
                    shape = CircleShape
                ) {
                    IconButton(onClick = {
                        Toast.makeText(context, "Profile switching triggered", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = custom.textPrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video Playback & Data Section
        SettingsSectionTitle("VIDEO PLAYBACK & DATA")

        SettingsClickItem(
            icon = Icons.Filled.DataUsage,
            title = "Cellular Data Usage",
            subtitle = cellularDataMode,
            onClick = { showDataUsageDialog = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Downloads Section
        SettingsSectionTitle("DOWNLOADS")

        SettingsSwitchItem(
            icon = Icons.Filled.Wifi,
            title = "Wi-Fi Only Downloads",
            subtitle = "Save mobile data when downloading content",
            checked = downloadWifiOnly,
            onCheckedChange = { downloadWifiOnly = it }
        )

        SettingsClickItem(
            icon = Icons.Filled.HighQuality,
            title = "Download Video Quality",
            subtitle = downloadQuality,
            onClick = {
                downloadQuality = if (downloadQuality.contains("High")) "Standard (720p)" else "High (1080p)"
                Toast.makeText(context, "Quality set to $downloadQuality", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Notifications Section
        SettingsSectionTitle("NOTIFICATIONS")

        SettingsSwitchItem(
            icon = Icons.Filled.Notifications,
            title = "New Episode Alerts",
            subtitle = "Get notified when new episodes of your saved anime drop",
            checked = notifyNewReleases,
            onCheckedChange = { notifyNewReleases = it }
        )

        SettingsSwitchItem(
            icon = Icons.Filled.ThumbUp,
            title = "Personal Recommendations",
            subtitle = "Occasional suggestions based on your watch history",
            checked = notifyRecommendations,
            onCheckedChange = { notifyRecommendations = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Theme & Storage Section
        SettingsSectionTitle("THEME & STORAGE")

        SettingsClickItem(
            icon = Icons.Filled.Palette,
            title = "App Color Theme",
            subtitle = "Obsidian Dark & Neon Lime (Active)",
            onClick = {
                Toast.makeText(context, "Theme set to Obsidian Dark & Neon Lime", Toast.LENGTH_SHORT).show()
            }
        )

        SettingsClickItem(
            icon = Icons.Filled.DeleteSweep,
            title = "Clear Cache",
            subtitle = "Used cache space: ${cacheSizeMb}MB",
            onClick = {
                cacheSizeMb = 0
                Toast.makeText(context, "Cache successfully cleared!", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        Button(
            onClick = { showSignOutDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = custom.surface,
                contentColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out of AnimK", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Version info
        Text(
            text = "AnimK Version 2.4.0 (Netflix Edition)",
            color = custom.textMuted,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // Cellular Data Usage Dialog
    if (showDataUsageDialog) {
        AlertDialog(
            onDismissRequest = { showDataUsageDialog = false },
            title = { Text("Cellular Data Usage", color = custom.textPrimary) },
            text = {
                Column {
                    listOf("Automatic", "Wi-Fi Only", "Save Data", "Maximum Data").forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    cellularDataMode = mode
                                    showDataUsageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = mode,
                                color = if (cellularDataMode == mode) custom.primary else custom.textPrimary,
                                fontWeight = if (cellularDataMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                            if (cellularDataMode == mode) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = custom.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDataUsageDialog = false }) {
                    Text("Close", color = custom.primary)
                }
            },
            containerColor = custom.surface
        )
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out", color = custom.textPrimary) },
            text = { Text("Are you sure you want to sign out of your AnimK account?", color = custom.textSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        Toast.makeText(context, "Signed out of AnimK", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = custom.textPrimary)
                }
            },
            containerColor = custom.surface
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = LocalCustomColors.current.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsClickItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val custom = LocalCustomColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = custom.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = custom.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = custom.textSecondary, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = custom.textMuted)
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val custom = LocalCustomColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = custom.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = custom.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = custom.textSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = custom.onPrimary,
                checkedTrackColor = custom.primary,
                uncheckedThumbColor = custom.textMuted,
                uncheckedTrackColor = custom.cardSurface
            )
        )
    }
}
