package com.kairos.os.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
import com.kairos.os.ui.AppConnection
import com.kairos.os.ui.viewmodels.IntentViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    intentViewModel: IntentViewModel,
    installedApps: List<AppConnection>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val googleSansFont = FontFamily(Font(R.font.google_sans_regular, FontWeight.Normal))
    
    val userSettings by intentViewModel.userSettings.collectAsState()
    val appConfigsMap by intentViewModel.appConfigsMap.collectAsState()
    val distractingAppIds by intentViewModel.distractingAppIds.collectAsState()

    var sliderValue by remember(userSettings.dailyLeisureMinutes) {
        mutableFloatStateOf(userSettings.dailyLeisureMinutes.toFloat())
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            toastMessage = null
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

            // === SECTION 1: Daily Leisure Time ===
            Text(
                text = "DAILY LEISURE TIME",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = googleSansFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${sliderValue.roundToInt()}",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = googleSansFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "minutes / day",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = googleSansFont
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = {
                    val minutes = sliderValue.roundToInt()
                    intentViewModel.updateDailyLeisureTime(minutes) { _, msg ->
                        toastMessage = msg
                    }
                },
                valueRange = 15f..180f,
                steps = 10,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Text(
                text = "Global time budget allocated for all distracting apps per day. Apps cannot be opened once limit is reached.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = googleSansFont,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (userSettings.pendingLeisureMinutes != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "⏳ Increase to ${userSettings.pendingLeisureMinutes}m pending 12h cooling-off period.",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(20.dp))

            // === SECTION 2: App Classification ===
            Text(
                text = "DISTRACTING APPS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = googleSansFont,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Toggle on apps to enforce intent friction & timer gates before opening.",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(installedApps, key = { it.id }) { app ->
                    val appConfig = appConfigsMap[app.id.lowercase()]
                    val isDistracting = distractingAppIds.contains(app.id.lowercase())
                    val isPending = appConfig?.pendingCategory != null

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
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

                            Column {
                                Text(
                                    text = app.displayName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = googleSansFont,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (isPending) {
                                    Text(
                                        text = "Change pending (12h cooling-off)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Text(
                                        text = if (isDistracting) "Distracting (Intent Gate active)" else "Utility",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                                        color = if (isDistracting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Switch(
                            checked = isDistracting,
                            onCheckedChange = { checked ->
                                intentViewModel.toggleAppDistracting(app.id, app.packageName, checked) { msg ->
                                    toastMessage = msg
                                }
                            },
                            enabled = !isPending,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}
