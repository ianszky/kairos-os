package com.kairos.os.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kairos.os.R
import com.kairos.os.domain.usecases.NotificationAppRule
import com.kairos.os.ui.AppConnection
import com.kairos.os.ui.viewmodels.IntentViewModel

@Composable
fun NotificationRulesScreen(
    intentViewModel: IntentViewModel,
    installedApps: List<AppConnection>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val googleSansFont = FontFamily(Font(R.font.google_sans_regular, FontWeight.Normal))
    
    val appNotificationRules by intentViewModel.appNotificationRules.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            toastMessage = null
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { 
                it.displayName.contains(searchQuery, ignoreCase = true) || 
                it.id.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "NOTIFICATION RULES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = googleSansFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure how KAIROS OS handles incoming notifications for each app:",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Explanation cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "• Allowed (Whitelisted): Always delivered directly on-device.",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "• Blocked (Blacklisted): Silently dismissed without saving to digest.",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "• Kai Decides: On-device Gemma AI classifies as CRITICAL or DIGEST.",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search app...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredApps, key = { it.id }) { app ->
                    val packageName = app.id
                    val currentRule = appNotificationRules[packageName] ?: NotificationAppRule.KAI_DECIDES

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (app.iconDrawable != null || app.iconUrl != null) {
                                    AsyncImage(
                                        model = app.iconDrawable ?: app.iconUrl,
                                        contentDescription = app.displayName,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = app.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = googleSansFont,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = googleSansFont,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = app.id,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = googleSansFont,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3-Way Segmented Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RuleOptionSegment(
                                label = "Allowed",
                                isSelected = currentRule == NotificationAppRule.ALLOWED,
                                activeColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    intentViewModel.setAppNotificationRule(packageName, NotificationAppRule.ALLOWED)
                                    toastMessage = "${app.displayName} set to Allowed"
                                }
                            )

                            RuleOptionSegment(
                                label = "Blocked",
                                isSelected = currentRule == NotificationAppRule.BLOCKED,
                                activeColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    intentViewModel.setAppNotificationRule(packageName, NotificationAppRule.BLOCKED)
                                    toastMessage = "${app.displayName} set to Blocked"
                                }
                            )

                            RuleOptionSegment(
                                label = "Kai Decides",
                                isSelected = currentRule == NotificationAppRule.KAI_DECIDES,
                                activeColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    intentViewModel.setAppNotificationRule(packageName, NotificationAppRule.KAI_DECIDES)
                                    toastMessage = "${app.displayName} set to Kai Decides"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleOptionSegment(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val googleSansFont = FontFamily(Font(R.font.google_sans_regular, FontWeight.Normal))

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = googleSansFont,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
