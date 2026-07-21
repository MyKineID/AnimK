package com.animk.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.animk.app.data.repository.AuthRepository
import com.animk.app.ui.theme.AppThemeAccent
import com.animk.app.ui.theme.LocalCustomColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    activeAccent: AppThemeAccent,
    onAccentChange: (AppThemeAccent) -> Unit,
    onOpenAuthSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val custom = LocalCustomColors.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }

    var showThemeSheet by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(authRepository.isUserLoggedIn()) }
    val currentUser = authRepository.getCurrentUser()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(custom.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = custom.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Profile / Supabase Auth Card
        Surface(
            color = custom.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(custom.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLoggedIn) (currentUser?.email?.take(1)?.uppercase() ?: "A") else "A",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = custom.onPrimary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoggedIn) (currentUser?.email ?: "AnimK Member") else "Guest User",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = custom.textPrimary
                    )
                    Text(
                        text = if (isLoggedIn) "Supabase Account Verified" else "Sign in for comments & sync",
                        fontSize = 12.sp,
                        color = custom.textSecondary
                    )
                }

                if (isLoggedIn) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                authRepository.signOut()
                                isLoggedIn = false
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color(0xFFEF5350))
                    }
                } else {
                    Button(
                        onClick = onOpenAuthSheet,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = custom.primary,
                            contentColor = custom.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Login", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // App Accent Theme Selector Button
        Surface(
            color = custom.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showThemeSheet = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Palette, contentDescription = null, tint = custom.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("App Theme Accent", fontWeight = FontWeight.SemiBold, color = custom.textPrimary)
                        Text(activeAccent.displayName, fontSize = 12.sp, color = custom.textSecondary)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(activeAccent.color)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // High Refresh Rate Toggle Card
        Surface(
            color = custom.surface,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Speed, contentDescription = null, tint = custom.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Dynamic High Refresh Rate", fontWeight = FontWeight.SemiBold, color = custom.textPrimary)
                        Text("60Hz / 90Hz / 120Hz display optimization", fontSize = 12.sp, color = custom.textSecondary)
                    }
                }
                Switch(
                    checked = true,
                    onCheckedChange = {},
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = custom.onPrimary,
                        checkedTrackColor = custom.primary
                    )
                )
            }
        }
    }

    // Animal Theme Picker Sheet
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            containerColor = custom.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Animal Theme",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = custom.textPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(AppThemeAccent.entries) { theme ->
                        val isSelected = activeAccent == theme
                        Surface(
                            color = if (isSelected) theme.color.copy(alpha = 0.2f) else custom.cardSurface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) theme.color else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onAccentChange(theme)
                                    showThemeSheet = false
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(theme.color)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = theme.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = custom.textPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
