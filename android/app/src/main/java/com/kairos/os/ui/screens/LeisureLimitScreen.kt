package com.kairos.os.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kairos.os.R
import com.kairos.os.ui.viewmodels.IntentViewModel
import kotlin.math.roundToInt

@Composable
fun LeisureLimitScreen(
    intentViewModel: IntentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val googleSansFont = FontFamily(Font(R.font.google_sans_regular, FontWeight.Normal))
    val userSettings by intentViewModel.userSettings.collectAsState()
    val isLoading by intentViewModel.isLoading.collectAsState()

    var sliderValue by remember(userSettings.dailyLeisureMinutes, userSettings.pendingLeisureMinutes) {
        mutableFloatStateOf(
            (userSettings.pendingLeisureMinutes ?: userSettings.dailyLeisureMinutes).toFloat()
        )
    }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        intentViewModel.loadData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp)
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 15f..180f,
                steps = 10,
                enabled = !isSaving && !isLoading,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = context.getString(R.string.leisure_limit_tomorrow_notice),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    isSaving = true
                    val minutes = sliderValue.roundToInt()
                    intentViewModel.updateDailyLeisureTime(minutes) { success, message ->
                        isSaving = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        if (success) onBack()
                    }
                },
                enabled = !isSaving && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f)
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Save for tomorrow",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}
