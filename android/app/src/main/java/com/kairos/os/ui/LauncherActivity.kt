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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.Font
import com.kairos.os.R
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import coil.compose.AsyncImage
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import coil.ImageLoader
import coil.decode.SvgDecoder
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke

val dotoFont = FontFamily(
    Font(R.font.doto_regular, FontWeight.Normal),
    Font(R.font.doto_bold, FontWeight.Bold)
)

val googleSansFont = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_bold, FontWeight.Bold)
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
    titleLarge = TextStyle(fontFamily = googleSansFont, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    bodyLarge = TextStyle(fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.08.sp)
)

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    @Inject
    lateinit var apiClient: com.kairos.os.data.api.KairosApiClient

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
                            onLogout = { authViewModel.signOut() },
                            apiClient = apiClient
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

data class AppConnection(
    val id: String,
    val displayName: String,
    val iconUrl: String? = null,
    val iconDrawable: android.graphics.drawable.Drawable? = null,
    val category: String,
    val packageName: String? = null
)

val composioApps = listOf(
    AppConnection("gmail", "Gmail", "https://logos.composio.dev/api/gmail", null, "productivity"),
    AppConnection("composio", "Composio", "https://logos.composio.dev/api/composio", null, "developer"),
    AppConnection("github", "Github", "https://logos.composio.dev/api/github", null, "developer"),
    AppConnection("googlecalendar", "Google Calendar", "https://logos.composio.dev/api/googlecalendar", null, "productivity"),
    AppConnection("notion", "Notion", "https://logos.composio.dev/api/notion", null, "productivity"),
    AppConnection("googlesheets", "Google Sheets", "https://logos.composio.dev/api/googlesheets", null, "productivity"),
    AppConnection("googledocs", "Google Docs", "https://logos.composio.dev/api/googledocs", null, "productivity"),
    AppConnection("googlecontacts", "Google Contacts", "https://logos.composio.dev/api/googlecontacts", null, "productivity"),
    AppConnection("googleforms", "Google Forms", "https://logos.composio.dev/api/googleforms", null, "productivity"),
    AppConnection("googledrive", "Google Drive", "https://logos.composio.dev/api/googledrive", null, "storage"),
    AppConnection("googletasks", "Google Tasks", "https://logos.composio.dev/api/googletasks", null, "productivity"),
    AppConnection("googlemaps", "Google Maps", "https://logos.composio.dev/api/googlemaps", null, "utility"),
    AppConnection("googlesuper", "Google Super", "https://logos.composio.dev/api/googlesuper", null, "productivity"),
    AppConnection("googlechat", "Google Chat", "https://logos.composio.dev/api/googlechat", null, "communication"),
    AppConnection("googleclassroom", "Google Classroom", "https://logos.composio.dev/api/googleclassroom", null, "productivity"),
    AppConnection("googleslides", "Google Slides", "https://logos.composio.dev/api/googleslides", null, "productivity"),
    AppConnection("googlephotos", "Google Photos", "https://logos.composio.dev/api/googlephotos", null, "utility"),
    AppConnection("googlemeet", "Google Meet", "https://logos.composio.dev/api/googlemeet", null, "communication"),
    AppConnection("slack", "Slack", "https://logos.composio.dev/api/slack", null, "communication"),
    AppConnection("supabase", "Supabase", "https://logos.composio.dev/api/supabase", null, "developer"),
    AppConnection("outlook", "Outlook", "https://logos.composio.dev/api/outlook", null, "productivity"),
    AppConnection("twitter", "Twitter", "https://logos.composio.dev/api/twitter", null, "social"),
    AppConnection("hubspot", "HubSpot", "https://logos.composio.dev/api/hubspot", null, "crm"),
    AppConnection("linear", "Linear", "https://logos.composio.dev/api/linear", null, "productivity"),
    AppConnection("airtable", "Airtable", "https://logos.composio.dev/api/airtable", null, "productivity"),
    AppConnection("jira", "Jira", "https://logos.composio.dev/api/jira", null, "productivity"),
    AppConnection("youtube", "Youtube", "https://logos.composio.dev/api/youtube", null, "media"),
    AppConnection("slackbot", "Slackbot", "https://logos.composio.dev/api/slackbot", null, "communication"),
    AppConnection("canvas", "Canvas", "https://logos.composio.dev/api/canvas", null, "education"),
    AppConnection("bitbucket", "Bitbucket", "https://logos.composio.dev/api/bitbucket", null, "developer"),
    AppConnection("discord", "Discord", "https://logos.composio.dev/api/discord", null, "communication"),
    AppConnection("figma", "Figma", "https://logos.composio.dev/api/figma", null, "design"),
    AppConnection("reddit", "Reddit", "https://logos.composio.dev/api/reddit", null, "social"),
    AppConnection("browser", "Composio Search", "https://logos.composio.dev/api/browser", null, "utility"),
    AppConnection("hackernews", "Hacker News", "https://logos.composio.dev/api/hackernews", null, "news"),
    AppConnection("microsoftteams", "Microsoft Teams", "https://logos.composio.dev/api/microsoftteams", null, "communication"),
    AppConnection("asana", "Asana", "https://logos.composio.dev/api/asana", null, "productivity"),
    AppConnection("shopify", "Shopify", "https://logos.composio.dev/api/shopify", null, "commerce"),
    AppConnection("linkedin", "LinkedIn", "https://logos.composio.dev/api/linkedin", null, "social"),
    AppConnection("onedrive", "OneDrive", "https://logos.composio.dev/api/onedrive", null, "storage"),
    AppConnection("docusign", "DocuSign", "https://logos.composio.dev/api/docusign", null, "productivity"),
    AppConnection("discordbot", "Discord Bot", "https://logos.composio.dev/api/discordbot", null, "communication"),
    AppConnection("salesforce", "Salesforce", "https://logos.composio.dev/api/salesforce", null, "crm"),
    AppConnection("calendly", "Calendly", "https://logos.composio.dev/api/calendly", null, "productivity"),
    AppConnection("trello", "Trello", "https://logos.composio.dev/api/trello", null, "productivity"),
    AppConnection("dropbox", "Dropbox", "https://logos.composio.dev/api/dropbox", null, "storage")
)

@Composable
fun MindfulLauncherScreen(
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onLogout: () -> Unit = {},
    apiClient: com.kairos.os.data.api.KairosApiClient
) {
    val chatViewModel: com.kairos.os.ui.viewmodels.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val conversations by chatViewModel.conversations.collectAsState()
    val currentConversationId by chatViewModel.currentConversationId.collectAsState()
    val currentMessages by chatViewModel.currentMessages.collectAsState()

    LaunchedEffect(Unit) {
        chatViewModel.loadConversations()
    }

    var isSidebarOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    var termInput by remember { mutableStateOf("") }
    
    var isFrictionMode by remember { mutableStateOf(false) }
    var selectedFrictionTime by remember { mutableStateOf<String?>(null) }
    var frictionReason by remember { mutableStateOf("") }
    
    var isAppDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    var isTerminalFocused by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val packageManager = context.packageManager
    val installedApps = remember {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(packageManager).toString()
            val icon = resolveInfo.loadIcon(packageManager)
            
            AppConnection(
                id = appName.lowercase().replace(" ", "-"),
                displayName = appName,
                iconDrawable = icon,
                category = "installed",
                packageName = packageName
            )
        }.sortedBy { it.displayName }
    }

    val availableApps = remember { (composioApps + installedApps).sortedBy { it.displayName } }
    
    val parsedActiveApp = remember(termInput) {
        val firstWord = termInput.substringBefore(' ')
        if (firstWord.startsWith("@")) {
            val slug = firstWord.drop(1)
            if (availableApps.any { it.id.equals(slug, ignoreCase = true) }) slug.lowercase() else null
        } else null
    }

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    LaunchedEffect(termInput) {
        val atIndex = termInput.lastIndexOf('@')
        if (atIndex != -1) {
            val query = termInput.substring(atIndex + 1)
            if (!query.contains(" ")) {
                isAppDrawerOpen = true
                searchQuery = query.lowercase()
            } else {
                isAppDrawerOpen = false
            }
        } else {
            isAppDrawerOpen = false
        }
    }

    val interactions = remember { mutableStateListOf<com.kairos.os.domain.models.Interaction>() }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(currentMessages) {
        if (currentMessages.isNotEmpty() || currentConversationId != null) {
            interactions.clear()
            currentMessages.forEach { msg ->
                if (msg.role == "user") {
                    interactions.add(com.kairos.os.domain.models.Interaction.UserCommand(msg.content, msg.appTarget))
                } else {
                    val response = com.kairos.os.domain.models.KairosResponse(
                        type = if (msg.widgetPayload != null) "WIDGET" else "TEXT",
                        text = msg.content,
                        widget = msg.widgetPayload
                    )
                    interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(response))
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().haze(hazeState)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = 450.dp.toPx()
            val center = Offset(size.width / 2, size.height * 0.95f)
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFFF6B00).copy(alpha = 0.25f),
                    0.4f to Color(0xFFFF4600).copy(alpha = 0.10f),
                    0.85f to Color.Transparent,
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        val screenOffset = if (isSidebarOpen) 280f else if (isSettingsOpen) -280f else 0f
        val animatedOffset by animateFloatAsState(targetValue = screenOffset, label = "ScreenOffset")
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = animatedOffset.dp)
                .statusBarsPadding()
                .imePadding()
        ) {
            // Main Content
            if (!isChatOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ClockView()
                }
            } else {
                ChatView(interactions = interactions)
            }

            // Header (Aligned to TopCenter, fading vertical gradient background)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                            0.6f to MaterialTheme.colorScheme.background.copy(alpha = 0.70f),
                            1.0f to Color.Transparent
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = { isSidebarOpen = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "System Logs", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isChatOpen) {
                            IconButton(onClick = { 
                                isChatOpen = false 
                                chatViewModel.startNewConversation()
                                interactions.clear()
                            }) {
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
            }

            // Input box column (Aligned to BottomCenter, fading bottom gradient background)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                AnimatedVisibility(visible = isAppDrawerOpen) {
                    val filteredApps = availableApps.filter { 
                        it.id.contains(searchQuery, ignoreCase = true) || 
                        it.displayName.contains(searchQuery, ignoreCase = true) 
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "AVAILABLE CONNECTIONS", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(filteredApps.size) { index ->
                                val app = filteredApps[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            termInput = "@${app.id} "
                                            isAppDrawerOpen = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = app.iconDrawable ?: app.iconUrl,
                                        imageLoader = imageLoader,
                                        contentDescription = app.displayName,
                                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(app.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                                        Text("@${app.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(if (app.packageName != null) "App" else "Integration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            if (filteredApps.isEmpty()) {
                                item {
                                    Text(
                                        "No connections found matching '@$searchQuery'", 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .hazeChild(state = hazeState, shape = RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp, 
                                if (isTerminalFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), 
                                RoundedCornerShape(16.dp)
                            )
                            .padding(20.dp)
                    ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(">", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        val mentionVisualTransformation = remember {
                            VisualTransformation { text ->
                                val firstWord = text.text.substringBefore(' ')
                                val builder = androidx.compose.ui.text.AnnotatedString.Builder(text.text)
                                if (firstWord.startsWith("@")) {
                                    builder.addStyle(SpanStyle(color = Color(0xFFFF6B00)), 0, firstWord.length)
                                }
                                TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
                            }
                        }

                        BasicTextField(
                            value = termInput,
                            onValueChange = { termInput = it },
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 100.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isTerminalFocused = it.isFocused }
                                .drawBehind {
                                    val layoutResult = textLayoutResult ?: return@drawBehind
                                    val firstWord = termInput.substringBefore(' ')
                                    if (firstWord.startsWith("@") && firstWord.length > 1 && layoutResult.layoutInput.text.text.startsWith(firstWord)) {
                                        try {
                                            val start = layoutResult.getBoundingBox(0)
                                            val end = layoutResult.getBoundingBox(firstWord.length - 1)
                                            val bgRect = androidx.compose.ui.geometry.Rect(
                                                left = start.left,
                                                top = start.top,
                                                right = end.right,
                                                bottom = start.bottom
                                            )
                                            val paddedRect = bgRect.inflate(2.dp.toPx())
                                            val primaryColor = Color(0xFFFF6B00)
                                            drawRoundRect(
                                                color = primaryColor.copy(alpha = 0.2f),
                                                topLeft = paddedRect.topLeft,
                                                size = paddedRect.size,
                                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                            )
                                            drawRoundRect(
                                                color = primaryColor.copy(alpha = 0.5f),
                                                topLeft = paddedRect.topLeft,
                                                size = paddedRect.size,
                                                style = Stroke(width = 1.dp.toPx()),
                                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                            )
                                        } catch (e: Exception) {
                                            // Ignore out of bounds
                                        }
                                    }
                                },
                            onTextLayout = { textLayoutResult = it },
                            visualTransformation = mentionVisualTransformation,
                            decorationBox = { innerTextField ->
                                if (termInput.isEmpty()) {
                                    Text("Type to search or command...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = TextStyle(fontFamily = googleSansFont, fontSize = 14.sp))
                                }
                                innerTextField()
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                if (termInput.isNotBlank() && !termInput.startsWith("/") && termInput != "@$parsedActiveApp") {
                                    val currentIntent = if (parsedActiveApp != null) termInput.substringAfter(' ').trim() else termInput
                                    val currentTarget = parsedActiveApp
                                    isChatOpen = true
                                    interactions.add(com.kairos.os.domain.models.Interaction.UserCommand(currentIntent, currentTarget))
                                    isLoading = true
                                    interactions.add(com.kairos.os.domain.models.Interaction.Loading())
                                    termInput = ""

                                    coroutineScope.launch {
                                        try {
                                            val response = apiClient.postPrompt(currentIntent, currentTarget, currentConversationId)
                                            chatViewModel.onPromptResponse(response.meta?.conversationId)
                                            interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                                            interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(response))
                                        } catch (e: Exception) {
                                            interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                                            interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(
                                                com.kairos.os.domain.models.KairosResponse(
                                                    type = "ERROR",
                                                    text = "Failed to connect to AI: ${e.message}"
                                                )
                                            ))
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            })
                        )
                        
                        val currentApp = availableApps.find { it.id == parsedActiveApp }
                        if (currentApp?.packageName != null) {
                            IconButton(onClick = {
                                val launchIntent = packageManager.getLaunchIntentForPackage(currentApp.packageName!!)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                }
                            }) {
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open App", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        IconButton(onClick = {
                            if (termInput.isNotBlank() && !termInput.startsWith("/") && termInput != "@$parsedActiveApp") {
                                val currentIntent = if (parsedActiveApp != null) termInput.substringAfter(' ').trim() else termInput
                                val currentTarget = parsedActiveApp
                                isChatOpen = true
                                interactions.add(com.kairos.os.domain.models.Interaction.UserCommand(currentIntent, currentTarget))
                                isLoading = true
                                interactions.add(com.kairos.os.domain.models.Interaction.Loading())
                                termInput = ""

                                coroutineScope.launch {
                                    try {
                                        val response = apiClient.postPrompt(currentIntent, currentTarget, currentConversationId)
                                        chatViewModel.onPromptResponse(response.meta?.conversationId)
                                        interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                                        interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(response))
                                    } catch (e: Exception) {
                                        interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                                        interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(
                                            com.kairos.os.domain.models.KairosResponse(
                                                type = "ERROR",
                                                text = "Failed to connect to AI: ${e.message}"
                                            )
                                        ))
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
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
                                        append("${parsedActiveApp ?: "unknown"}.")
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
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                decorationBox = { innerTextField ->
                                    if (frictionReason.isEmpty()) {
                                        Text("[reason] (e.g. check messages)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = TextStyle(fontFamily = googleSansFont, fontSize = 14.sp))
                                    }
                                    innerTextField()
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val isLaunchValid = selectedFrictionTime != null && frictionReason.trim().length > 2
                            Button(
                                onClick = { termInput = ""; frictionReason = ""; selectedFrictionTime = null },
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
                    
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(conversations.size) { index ->
                            val conv = conversations[index]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isSidebarOpen = false
                                        chatViewModel.selectConversation(conv.id)
                                        isChatOpen = true
                                    }
                                    .padding(vertical = 16.dp)
                            ) {
                                val displayTitle = conv.title ?: "New Conversation"
                                Text(displayTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(8.dp))
                                // Very basic date formatter fallback since we have ISO string
                                val displayDate = conv.createdAt.take(10)
                                Text(displayDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                        }
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
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = dotoFont, fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatView(interactions: MutableList<com.kairos.os.domain.models.Interaction>) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    LaunchedEffect(interactions.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(90.dp))
        Spacer(modifier = Modifier.weight(1f))
        
        interactions.forEach { interaction ->
            when (interaction) {
                is com.kairos.os.domain.models.Interaction.UserCommand -> {
                    val prefix = if (interaction.appTarget != null) "@${interaction.appTarget} " else ""
                    ChatBubble(isUser = true, text = "> $prefix${interaction.command}")
                }
                is com.kairos.os.domain.models.Interaction.AssistantResponse -> {
                    if (!interaction.response.text.isNullOrBlank()) {
                        ChatBubble(isUser = false, text = interaction.response.text)
                    }
                    if (interaction.response.widget != null) {
                        if (!interaction.response.text.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        com.kairos.os.ui.components.WidgetRenderer(
                            widget = interaction.response.widget,
                            onAction = { action ->
                                if (action.actionType == "DEEP_LINK") {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(action.target))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else if (action.actionType == "DISMISS") {
                                    interactions.remove(interaction)
                                }
                            }
                        )
                    }
                }
                is com.kairos.os.domain.models.Interaction.Loading -> {
                    com.kairos.os.ui.components.TypingIndicator()
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        Spacer(modifier = Modifier.height(130.dp))
    }
}

@Composable
fun ChatBubble(isUser: Boolean, text: String) {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val codeText = MaterialTheme.colorScheme.primary
    val annotatedText = remember(text, codeBg, codeText) {
        if (isUser) {
            AnnotatedString(text)
        } else {
            parseMarkdownToAnnotatedString(text, codeBg, codeText)
        }
    }
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
                .padding(
                    start = if (isUser) 18.dp else 0.dp,
                    end = if (isUser) 18.dp else 0.dp,
                    top = if (isUser) 14.dp else 8.dp,
                    bottom = if (isUser) 14.dp else 8.dp
                )
        ) {
            Text(
                text = annotatedText,
                color = if (isUser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                style = if (isUser) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal)
            )
        }
    }
}

fun parseMarkdownToAnnotatedString(
    text: String,
    codeBgColor: Color,
    codeTextColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        var inCodeBlock = false
        
        lines.forEachIndexed { index, line ->
            if (line.trim().startsWith("```")) {
                inCodeBlock = !inCodeBlock
                return@forEachIndexed
            }
            
            if (inCodeBlock) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor,
                        color = codeTextColor
                    )
                ) {
                    append(line)
                }
                if (index < lines.size - 1) {
                    append("\n")
                }
                return@forEachIndexed
            }
            
            var formattedLine = line
            var isHeading = false
            var headingLevel = 0
            if (line.startsWith("#")) {
                val match = Regex("^(#{1,6})\\s+(.*)$").find(line)
                if (match != null) {
                    headingLevel = match.groupValues[1].length
                    formattedLine = match.groupValues[2]
                    isHeading = true
                }
            }
            
            if (!isHeading && (line.startsWith("- ") || line.startsWith("* "))) {
                formattedLine = "• " + line.substring(2)
            }
            
            val headingStyle = when (headingLevel) {
                1 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                3 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)
                else -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            
            val pattern = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|`.*?`)")
            var currentIndex = 0
            val matches = pattern.findAll(formattedLine)
            
            for (match in matches) {
                if (match.range.first > currentIndex) {
                    val plainText = formattedLine.substring(currentIndex, match.range.first)
                    if (isHeading) {
                        withStyle(headingStyle) {
                            append(plainText)
                        }
                    } else {
                        append(plainText)
                    }
                }
                
                val token = match.value
                val innerText = when {
                    token.startsWith("**") && token.endsWith("**") -> token.substring(2, token.length - 2)
                    token.startsWith("*") && token.endsWith("*") -> token.substring(1, token.length - 1)
                    token.startsWith("`") && token.endsWith("`") -> token.substring(1, token.length - 1)
                    else -> token
                }
                
                val style = when {
                    token.startsWith("**") -> SpanStyle(fontWeight = FontWeight.Bold)
                    token.startsWith("*") -> SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    token.startsWith("`") -> SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBgColor,
                        color = codeTextColor
                    )
                    else -> SpanStyle()
                }
                
                withStyle(style.merge(if (isHeading) headingStyle else SpanStyle())) {
                    append(innerText)
                }
                
                currentIndex = match.range.last + 1
            }
            
            if (currentIndex < formattedLine.length) {
                val remainingText = formattedLine.substring(currentIndex)
                if (isHeading) {
                    withStyle(headingStyle) {
                        append(remainingText)
                    }
                } else {
                    append(remainingText)
                }
            }
            
            if (index < lines.size - 1) {
                append("\n")
            }
        }
    }
}
