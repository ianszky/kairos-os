package com.kairos.os.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.Font
import com.kairos.os.R
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

val dotoFont = FontFamily(
    Font(R.font.doto_regular, FontWeight.Normal),
    Font(R.font.doto_bold, FontWeight.Bold)
)

val LightKairosColors = lightColorScheme(
    primary = Color(0xFFFF6B00),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF9F9F9),
    onBackground = Color(0xFF0A0A0A),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFCCCCCC),
    onSurfaceVariant = Color(0xFF444444)
)

val DarkKairosColors = darkColorScheme(
    primary = Color(0xFFFF6B00),
    background = Color(0xFF050505),
    surface = Color(0xFF111111),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFF8A8A8A)
)

val KairosTypography = Typography(
    displayLarge = TextStyle(fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 72.sp, letterSpacing = (-2).sp),
    titleLarge = TextStyle(fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    bodyLarge = TextStyle(fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.08.sp)
)

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabaseClient.handleDeeplinks(intent)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            val colorScheme = if (isDarkTheme) DarkKairosColors else LightKairosColors
            MaterialTheme(colorScheme = colorScheme, typography = KairosTypography) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()
                    
                    if (sessionStatus is SessionStatus.Authenticated) {
                        val authViewModel: com.kairos.os.ui.viewmodels.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                        MindfulLauncherScreen(
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme },
                            onLogout = { authViewModel.signOut() }
                        )
                    } else {
                        var currentAuthScreen by remember { mutableStateOf("login") }
                        if (currentAuthScreen == "login") {
                            com.kairos.os.ui.screens.LoginScreen(
                                onLoginSuccess = { /* Automatically handled by session status */ },
                                onNavigateToSignUp = { currentAuthScreen = "signup" }
                            )
                        } else {
                            com.kairos.os.ui.screens.SignUpScreen(
                                onSignUpSuccess = { /* Automatically handled by session status */ },
                                onNavigateToLogin = { currentAuthScreen = "login" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        supabaseClient.handleDeeplinks(intent)
    }
}

@Composable
fun MindfulLauncherScreen(isDarkTheme: Boolean = true, onThemeToggle: () -> Unit = {}, onLogout: () -> Unit = {}) {
    var isSidebarOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }
    
    var termInput by remember { mutableStateOf("") }
    
    var activeApp by remember { mutableStateOf<String?>(null) }
    var isFrictionMode by remember { mutableStateOf(false) }
    var selectedFrictionTime by remember { mutableStateOf<String?>(null) }
    var frictionReason by remember { mutableStateOf("") }
    
    val prodApps = listOf("@google-docs", "@calendar", "@notes")
    val distApps = listOf("@instagram", "@youtube", "@facebook")

    val focusRequester = remember { FocusRequester() }
    var isTerminalFocused by remember { mutableStateOf(false) }

    LaunchedEffect(termInput) {
        val v = termInput.lowercase().trim()
        if (prodApps.contains(v)) {
            activeApp = v
            isFrictionMode = false
        } else if (distApps.contains(v)) {
            activeApp = v
            isFrictionMode = true
        } else {
            activeApp = null
            isFrictionMode = false
        }
    }

    val chatMessages = remember { mutableStateListOf<Pair<String, String>>() }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = 250.dp.toPx()
            val center = Offset(size.width / 2, size.height * 0.45f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFFF6B00).copy(alpha = 0.9f),
                    0.4f to Color(0xFFFF4600).copy(alpha = 0.4f),
                    0.75f to Color.Transparent,
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        val screenOffset = if (isSidebarOpen) 280f else if (isSettingsOpen) -280f else 0f
        val animatedOffset by animateFloatAsState(targetValue = screenOffset, label = "ScreenOffset")
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = animatedOffset.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { isSidebarOpen = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "System Logs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isChatOpen) {
                        IconButton(onClick = { isChatOpen = false }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = onThemeToggle) {
                        Icon(if (isDarkTheme) Icons.Default.Brightness4 else Icons.Default.Brightness7, contentDescription = "Toggle Theme", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { isSettingsOpen = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isChatOpen) {
                    ClockView()
                } else {
                    ChatView(messages = chatMessages)
                }
            }

            Column(modifier = Modifier.padding(24.dp)) {
                AnimatedVisibility(visible = activeApp != null && !isFrictionMode) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Text("QUICK ACCESS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .clickable { termInput = ""; activeApp = null }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row {
                                Text("Launch ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                                Text("${activeApp}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .border(
                            1.dp, 
                            if (isTerminalFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, 
                            RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(">", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = termInput,
                            onValueChange = { termInput = it },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isTerminalFocused = it.isFocused },
                            decorationBox = { innerTextField ->
                                if (termInput.isEmpty()) {
                                    Text("Type to search or command...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = TextStyle(fontFamily = dotoFont, fontSize = 14.sp))
                                }
                                innerTextField()
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                if (termInput.isNotBlank() && !termInput.startsWith("@") && !termInput.startsWith("/")) {
                                    isChatOpen = true
                                    chatMessages.add("user" to termInput)
                                    termInput = ""
                                }
                            })
                        )
                    }

                    AnimatedVisibility(visible = isFrictionMode) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                buildAnnotatedString {
                                    append("Intentional friction engaged for ")
                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                        append("$activeApp.")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("5m", "10m", "15m", "30m").forEach { time ->
                                    val isSelected = selectedFrictionTime == time
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.background,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                            .clickable { selectedFrictionTime = time }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(time, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            BasicTextField(
                                value = frictionReason,
                                onValueChange = { frictionReason = it },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                decorationBox = { innerTextField ->
                                    if (frictionReason.isEmpty()) {
                                        Text("[reason] (e.g. check messages)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = TextStyle(fontFamily = dotoFont, fontSize = 14.sp))
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val isLaunchValid = selectedFrictionTime != null && frictionReason.trim().length > 2
                            Button(
                                onClick = { termInput = ""; activeApp = null; frictionReason = ""; selectedFrictionTime = null },
                                enabled = isLaunchValid,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text("LAUNCH INTENT", style = MaterialTheme.typography.bodyMedium, letterSpacing = 0.08.sp)
                            }
                        }
                    }
                }
            }
        }

        if (isSidebarOpen || isSettingsOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isSidebarOpen = false
                        isSettingsOpen = false
                    }
            )
        }

        AnimatedVisibility(
            visible = isSidebarOpen,
            enter = slideInVertically(initialOffsetY = { 0 }) + fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp)
                ) {
                    Text("SYSTEM.LOGS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.1.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val logs = listOf("> explain the concept of intentional friction" to "TODAY 09:42", "> summarize weekly screen time" to "YESTERDAY 18:30", "> /open @instagram 5m" to "MON 14:15")
                    logs.forEach { (title, time) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isSidebarOpen = false
                                    isChatOpen = true
                                }
                                .padding(vertical = 16.dp)
                        ) {
                            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background)
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(24.dp)
                ) {
                    Text("SYS.CONFIG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.1.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val settings = listOf("Strict Mode" to true, "Monochrome Filter" to false, "Haptic Feedback" to true)
                    settings.forEach { (title, state) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            Box(
                                modifier = Modifier
                                    .size(40.dp, 20.dp)
                                    .background(Color.Transparent, RoundedCornerShape(10.dp))
                                    .border(1.dp, if (state) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (state) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                        .align(if (state) Alignment.CenterEnd else Alignment.CenterStart)
                                )
                            }
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("LOGOUT")
                    }
                }
            }
        }
    }
}

@Composable
fun ClockView() {
    var timeString by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while(true) {
            val date = Date()
            timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
            dateString = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(date).uppercase()
            delay(1000)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-40).dp)) {
        Box(modifier = Modifier.padding(bottom = 32.dp)) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                val foregroundColor = MaterialTheme.colorScheme.onBackground
                Canvas(modifier = Modifier.size(60.dp)) {
                    // SVG ViewBox is 130.12 x 134.04
                    // We scale to the canvas size
                    val scaleX = size.width / 130.12f
                    val scaleY = size.height / 134.04f
                    val r = 15.42f * scaleX // Assuming uniform scaling

                    val orange = Color(0xFFFF6B00)

                    // Draw the 6 circles
                    drawCircle(color = foregroundColor, radius = r, center = Offset(67.02f * scaleX, 67.02f * scaleY))
                    drawCircle(color = foregroundColor, radius = r, center = Offset(114.7f * scaleX, 118.62f * scaleY))
                    drawCircle(color = orange, radius = r, center = Offset(114.7f * scaleX, 15.42f * scaleY))
                    drawCircle(color = foregroundColor, radius = r, center = Offset(15.42f * scaleX, 15.42f * scaleY))
                    drawCircle(color = foregroundColor, radius = r, center = Offset(15.42f * scaleX, 67.02f * scaleY))
                    drawCircle(color = foregroundColor, radius = r, center = Offset(15.42f * scaleX, 118.62f * scaleY))
                }
            }
        }
        Text(
            text = timeString,
            style = MaterialTheme.typography.displayLarge.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color(0xFFFF6B00).copy(alpha = 0.2f),
                    blurRadius = 30f
                )
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatView(messages: List<Pair<String, String>>) {
    val scrollState = rememberScrollState()
    
    LaunchedEffect(messages.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        ChatBubble(isUser = true, text = "> explain the concept of intentional friction")
        Spacer(modifier = Modifier.height(24.dp))
        ChatBubble(isUser = false, text = "Intentional friction is the practice of adding deliberate delays or cognitive hurdles before accessing distracting environments. By requiring you to state an intention, it shifts your smartphone use from passive consumption to conscious engagement.")
        Spacer(modifier = Modifier.height(24.dp))
        
        messages.forEach { (type, text) ->
            ChatBubble(isUser = type == "user", text = if (type == "user") "> $text" else text)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ChatBubble(isUser: Boolean, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (isUser) MaterialTheme.colorScheme.surface else Color.Transparent,
                    if (isUser) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp) else RoundedCornerShape(0.dp)
                )
                .border(
                    if (isUser) 1.dp else 0.dp,
                    if (isUser) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                    if (isUser) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp) else RoundedCornerShape(0.dp)
                )
                .then(
                    if (!isUser) Modifier.border(width = 2.dp, color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(0.dp)).padding(start = 16.dp)
                    else Modifier
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = text,
                color = if (isUser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                style = if (isUser) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)
            )
        }
    }
}
