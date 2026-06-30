package com.kairos.os.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    KairosHomeScreen()
                }
            }
        }
    }
}

@Composable
fun KairosHomeScreen() {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current
    var interactions by remember { mutableStateOf(listOf<String>()) }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Chronological feed of interactions
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(interactions) { interaction ->
                Text(
                    text = interaction,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp),
                    style = TextStyle(fontSize = 16.sp)
                )
            }
        }

        // Text input field
        TextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Color.White
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (text.isNotBlank()) {
                        Toast.makeText(context, "Executing intent: $text", Toast.LENGTH_SHORT).show()
                        interactions = interactions + text
                        text = ""
                    }
                }
            )
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
