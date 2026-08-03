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
import android.util.Log
import io.github.jan.supabase.postgrest.postgrest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import io.github.jan.supabase.storage.storage
import com.kairos.os.data.api.AttachmentInfo
import androidx.compose.ui.graphics.asImageBitmap
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
    titleMedium = TextStyle(fontFamily = googleSansFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.25.sp),
    bodyMedium = TextStyle(fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = dotoFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.08.sp)
)

@AndroidEntryPoint
class LauncherActivity : ComponentActivity() {

    @Inject
    lateinit var supabaseClient: SupabaseClient

    @Inject
    lateinit var apiClient: com.kairos.os.data.api.KairosApiClient

    @Inject
    lateinit var localDigestGenerator: com.kairos.os.domain.usecases.LocalDigestGenerator

    @Inject
    lateinit var localAgentEngine: com.kairos.os.domain.usecases.LocalAgentEngine

    @Inject
    lateinit var localTitleGenerator: com.kairos.os.domain.usecases.LocalTitleGenerator

    @Inject
    lateinit var localNotesController: com.kairos.os.domain.tools.LocalNotesController

    @Inject
    lateinit var localCalendarController: com.kairos.os.domain.tools.LocalCalendarController

    @Inject
    lateinit var localAlarmController: com.kairos.os.domain.tools.LocalAlarmController

    @Inject
    lateinit var appSessionManager: com.kairos.os.domain.session.AppSessionManager

    @Inject
    lateinit var sessionCardHideStore: com.kairos.os.domain.session.SessionCardHideStore

    @Inject
    lateinit var agentNotificationNavigationStore: com.kairos.os.domain.navigation.AgentNotificationNavigationStore

    @Inject
    lateinit var localLlmClient: com.kairos.os.domain.usecases.LocalLlmClient

    @Inject
    lateinit var gemmaSttClient: com.kairos.os.domain.usecases.GemmaSttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supabaseClient.handleDeeplinks(intent)
        handleAgentNotificationIntent(intent)
        
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
                            apiClient = apiClient,
                            supabaseClient = supabaseClient,
                            localDigestGenerator = localDigestGenerator,
                            localAgentEngine = localAgentEngine,
                            localTitleGenerator = localTitleGenerator,
                            localNotesController = localNotesController,
                            localCalendarController = localCalendarController,
                            localAlarmController = localAlarmController,
                            appSessionManager = appSessionManager,
                            sessionCardHideStore = sessionCardHideStore,
                            agentNotificationNavigationStore = agentNotificationNavigationStore,
                            localLlmClient = localLlmClient,
                            gemmaSttClient = gemmaSttClient
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
        setIntent(intent)
        supabaseClient.handleDeeplinks(intent)
        handleAgentNotificationIntent(intent)
    }

    private fun handleAgentNotificationIntent(intent: android.content.Intent) {
        intent.getStringExtra(com.kairos.os.services.AgentNotificationHelper.EXTRA_OPEN_AGENT_ID)?.let { agentId ->
            agentNotificationNavigationStore.requestOpen(agentId)
            intent.removeExtra(com.kairos.os.services.AgentNotificationHelper.EXTRA_OPEN_AGENT_ID)
        }
    }
}

data class AppConnection(
    val id: String,
    val displayName: String,
    val iconUrl: String? = null,
    val iconDrawable: android.graphics.drawable.Drawable? = null,
    val category: String,
    val packageName: String? = null,
    val iconEmoji: String? = null
)

val localKaiApps = listOf(
    AppConnection("app:kainotes", "Kai Notes", null, null, "app", null, "📝"),
    AppConnection("app:kaicalendar", "Kai Calendar", null, null, "app", null, "📅"),
    AppConnection("app:kaiclock", "Kai Clock", null, null, "app", null, "⏰"),
    AppConnection("app:kaischeduled", "Kai Scheduled", null, null, "app", null, "🔄")
)

val composioApps = listOf(
    AppConnection("digest", "Digest Summary", null, null, "utility", null, "📰"),
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
    AppConnection("googlemaps", "Google Maps", "https://logos.composio.dev/api/google_maps", null, "utility"),
    AppConnection("googlesuper", "Google Super", "https://logos.composio.dev/api/googlesuper", null, "productivity"),
    AppConnection("googlechat", "Google Chat", "https://logos.composio.dev/api/googlechat", null, "communication"),
    AppConnection("googleclassroom", "Google Classroom", "https://logos.composio.dev/api/google_classroom", null, "productivity"),
    AppConnection("googleslides", "Google Slides", "https://logos.composio.dev/api/googleslides", null, "productivity"),
    AppConnection("googlephotos", "Google Photos", "https://logos.composio.dev/api/googlephotos", null, "utility"),
    AppConnection("googlemeet", "Google Meet", "https://logos.composio.dev/api/googlemeet", null, "communication"),
    AppConnection("slack", "Slack", "https://logos.composio.dev/api/slack", null, "communication"),
    AppConnection("supabase", "Supabase", "https://logos.composio.dev/api/supabase", null, "developer"),
    AppConnection("outlook", "Outlook", "https://logos.composio.dev/api/outlook", null, "productivity"),
    AppConnection("x", "X", "https://logos.composio.dev/api/twitter", null, "social"),
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
    AppConnection("browser", "Composio Search", "https://logos.composio.dev/api/composio", null, "utility"),
    AppConnection("hackernews", "Hacker News", "https://logos.composio.dev/api/hackernews", null, "news"),
    AppConnection("microsoftteams", "Microsoft Teams", "https://logos.composio.dev/api/microsoft_teams", null, "communication"),
    AppConnection("asana", "Asana", "https://logos.composio.dev/api/asana", null, "productivity"),
    AppConnection("shopify", "Shopify", "https://logos.composio.dev/api/shopify", null, "commerce"),
    AppConnection("linkedin", "LinkedIn", "https://logos.composio.dev/api/linkedin", null, "social"),
    AppConnection("onedrive", "OneDrive", "https://logos.composio.dev/api/one_drive", null, "storage"),
    AppConnection("docusign", "DocuSign", "https://logos.composio.dev/api/docusign", null, "productivity"),
    AppConnection("discordbot", "Discord Bot", "https://logos.composio.dev/api/discordbot", null, "communication"),
    AppConnection("salesforce", "Salesforce", "https://logos.composio.dev/api/salesforce", null, "crm"),
    AppConnection("calendly", "Calendly", "https://logos.composio.dev/api/calendly", null, "productivity"),
    AppConnection("trello", "Trello", "https://logos.composio.dev/api/trello", null, "productivity"),
    AppConnection("dropbox", "Dropbox", "https://logos.composio.dev/api/dropbox", null, "storage")
)

data class AttachmentState(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val uploading: Boolean = false,
    val uploadedPath: String? = null
)

fun getFileInfo(context: android.content.Context, uri: Uri): Pair<String, Long> {
    var name = "unknown"
    var size = 0L
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return Pair(name, size)
}

fun processVoiceText(text: String, availableApps: List<AppConnection>): String {
    if (text.isBlank()) return ""
    val words = text.split(" ")
    return words.joinToString(" ") { word ->
        val cleanWord = word.replace(Regex("[^a-zA-Z0-9\\-]"), "").lowercase()
        val matchingApp = availableApps.find { it.id == cleanWord || it.displayName.lowercase() == cleanWord }
        if (matchingApp != null) {
            val startPunc = word.takeWhile { !it.isLetterOrDigit() }
            val endPunc = word.takeLastWhile { !it.isLetterOrDigit() }
            "$startPunc@${matchingApp.id}$endPunc"
        } else {
            word
        }
    }
}

fun insertAppMention(currentInput: String, appId: String): String {
    val atIndex = currentInput.lastIndexOf('@')
    return if (atIndex != -1) {
        currentInput.substring(0, atIndex) + "@$appId "
    } else {
        if (currentInput.isEmpty() || currentInput.endsWith(" ")) {
            currentInput + "@$appId "
        } else {
            currentInput + " @$appId "
        }
    }
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMentionsPills(
    layoutResult: androidx.compose.ui.text.TextLayoutResult?,
    termInput: String,
    availableApps: List<AppConnection>,
    validStartsState: List<Int>,
    iconCache: Map<String, android.graphics.drawable.Drawable>
) {
    if (layoutResult == null) return
    val regex = Regex("@((?:app:)?[a-zA-Z0-9\\-]+)")
    val matches = regex.findAll(termInput).toList()
    
    var matchIndex = 0
    matches.forEach { match ->
        val appId = match.groups[1]?.value?.lowercase() ?: ""
        val app = availableApps.find { it.id == appId }
        if (app != null && matchIndex < validStartsState.size) {
            val originalStart = match.range.first
            val originalEnd = match.range.last + 1
            
            val spaceTransformed = validStartsState[matchIndex] + matchIndex
            val lastTransformed = spaceTransformed + (originalEnd - originalStart)
            
            try {
                val spaceRect = layoutResult.getBoundingBox(spaceTransformed)
                val wordEndRect = layoutResult.getBoundingBox(lastTransformed)
                
                val drawable = app.iconDrawable ?: iconCache[app.id]
                if (drawable != null) {
                    val iconSize = 14.dp.toPx()
                    val iconLeft = spaceRect.left + (spaceRect.width - iconSize) / 2
                    val iconTop = spaceRect.top + (spaceRect.height - iconSize) / 2
                    
                    drawable.bounds = android.graphics.Rect(
                        iconLeft.toInt(),
                        iconTop.toInt(),
                        (iconLeft + iconSize).toInt(),
                        (iconTop + iconSize).toInt()
                    )
                    drawable.draw(drawContext.canvas.nativeCanvas)
                } else if (app.iconEmoji != null) {
                    val paint = android.graphics.Paint().apply {
                        textSize = 11.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    val iconX = spaceRect.left + spaceRect.width / 2
                    val iconY = spaceRect.top + spaceRect.height / 2 + (paint.textSize / 3)
                    drawContext.canvas.nativeCanvas.drawText(app.iconEmoji, iconX, iconY, paint)
                }
            } catch (e: Exception) {
                // ignore layout out of bounds
            }
            matchIndex++
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MindfulLauncherScreen(
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onLogout: () -> Unit = {},
    apiClient: com.kairos.os.data.api.KairosApiClient,
    supabaseClient: io.github.jan.supabase.SupabaseClient,
    localDigestGenerator: com.kairos.os.domain.usecases.LocalDigestGenerator,
    localAgentEngine: com.kairos.os.domain.usecases.LocalAgentEngine,
    localTitleGenerator: com.kairos.os.domain.usecases.LocalTitleGenerator,
    localNotesController: com.kairos.os.domain.tools.LocalNotesController,
    localCalendarController: com.kairos.os.domain.tools.LocalCalendarController,
    localAlarmController: com.kairos.os.domain.tools.LocalAlarmController,
    appSessionManager: com.kairos.os.domain.session.AppSessionManager,
    sessionCardHideStore: com.kairos.os.domain.session.SessionCardHideStore,
    agentNotificationNavigationStore: com.kairos.os.domain.navigation.AgentNotificationNavigationStore,
    localLlmClient: com.kairos.os.domain.usecases.LocalLlmClient,
    gemmaSttClient: com.kairos.os.domain.usecases.GemmaSttClient
) {
    val chatViewModel: com.kairos.os.ui.viewmodels.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val conversations by chatViewModel.conversations.collectAsState()
    val currentConversationId by chatViewModel.currentConversationId.collectAsState()
    val currentMessages by chatViewModel.currentMessages.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val runningAgentsViewModel: com.kairos.os.ui.viewmodels.RunningAgentsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val runningAgents by runningAgentsViewModel.agents.collectAsState()
    val statusLines by runningAgentsViewModel.statusLines.collectAsState()
    var isAgentsExpanded by remember { mutableStateOf(false) }
    val runningJobs = remember { mutableStateMapOf<String, kotlinx.coroutines.Job>() }

    LaunchedEffect(Unit) {
        chatViewModel.loadConversations()
        runningAgentsViewModel.cleanupStale()
    }

    var isSidebarOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isChatOpen by remember { mutableStateOf(false) }
    
    val coroutineScope = rememberCoroutineScope()
    
    var termInput by remember { mutableStateOf("") }
    
    val intentViewModel: com.kairos.os.ui.viewmodels.IntentViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val scheduledViewModel: com.kairos.os.ui.viewmodels.ScheduledViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val userSettings by intentViewModel.userSettings.collectAsState()
    val distractingAppIds by intentViewModel.distractingAppIds.collectAsState()
    val activeSession by appSessionManager.activeSession.collectAsState()
    val hiddenSessionKeys by sessionCardHideStore.hiddenKeysFlow.collectAsState()
    val homeActivityItems = remember(runningAgents, activeSession, hiddenSessionKeys) {
        com.kairos.os.domain.models.buildHomeActivityItems(
            agents = runningAgents,
            session = activeSession,
            hideStore = sessionCardHideStore
        )
    }
    val pendingAgentId by agentNotificationNavigationStore.pendingAgentId.collectAsState()
    
    var selectedFrictionTime by remember { mutableStateOf<String?>(null) }
    var frictionReason by remember { mutableStateOf("") }
    var intentApproved by remember { mutableStateOf(false) }
    var isValidatingReason by remember { mutableStateOf(false) }
    var validationFeedback by remember { mutableStateOf<String?>(null) }


    
    var isAppDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    var isTerminalFocused by remember { mutableStateOf(false) }
    var activeScreen by remember { mutableStateOf("home") }
    var calendarViewMode by remember { mutableStateOf("week") }
    var noteIsEditing by remember { mutableStateOf(false) }
    var noteSaveState by remember { mutableStateOf(com.kairos.os.ui.screens.NoteSaveState.GRAY_CHECK) }
    var noteSaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var noteCancelAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val deletedConversationIds = remember { mutableStateListOf<String>() }
    val hazeState = remember { HazeState() }
    var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    var forceStackedLayout by remember { mutableStateOf(false) }
    var isPlusMenuOpen by remember { mutableStateOf(false) }
    
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
                id = "app:${appName.lowercase().replace(" ", "-")}",
                displayName = appName,
                iconDrawable = icon,
                category = "installed",
                packageName = packageName
            )
        }.sortedBy { it.displayName }
    }

    val availableApps = remember { (localKaiApps + composioApps + installedApps) }

    LaunchedEffect(availableApps, distractingAppIds) {
        intentViewModel.syncDistractingPackages(installedApps)
    }

    LaunchedEffect(Unit) {
        val activity = context as? LauncherActivity
        activity?.intent?.let { launchIntent ->
            if (launchIntent.getBooleanExtra(
                    com.kairos.os.services.SessionNotificationHelper.EXTRA_SESSION_EXPIRED,
                    false
                )
            ) {
                val appName = launchIntent.getStringExtra(
                    com.kairos.os.services.SessionNotificationHelper.EXTRA_EXPIRED_APP_NAME
                )
                validationFeedback = if (appName != null) {
                    "Leisure time ended for $appName."
                } else {
                    "Leisure time ended."
                }
                launchIntent.removeExtra(com.kairos.os.services.SessionNotificationHelper.EXTRA_SESSION_EXPIRED)
                launchIntent.removeExtra(com.kairos.os.services.SessionNotificationHelper.EXTRA_EXPIRED_APP_NAME)
            }
            launchIntent.getStringExtra(
                com.kairos.os.services.AgentNotificationHelper.EXTRA_OPEN_AGENT_ID
            )?.let { agentId ->
                agentNotificationNavigationStore.requestOpen(agentId)
                launchIntent.removeExtra(com.kairos.os.services.AgentNotificationHelper.EXTRA_OPEN_AGENT_ID)
            }
        }
    }

    LaunchedEffect(activeSession) {
        if (activeSession == null) {
            sessionCardHideStore.clearAll()
        }
    }

    var selectedDrawerTab by remember { mutableStateOf("Integrations") }
    val currentTabApps = remember(selectedDrawerTab, localKaiApps, composioApps, installedApps) {
        if (selectedDrawerTab == "App") {
            localKaiApps + installedApps
        } else {
            composioApps
        }
    }
    
    val selectedAttachments = remember { mutableStateListOf<AttachmentState>() }
    val iconCache = remember { mutableStateMapOf<String, android.graphics.drawable.Drawable>() }
    val validStartsState = remember { mutableStateListOf<Int>() }
    val mentionVisualTransformation = remember(availableApps) {
        MentionVisualTransformation(availableApps) { resolvedStarts ->
            validStartsState.clear()
            validStartsState.addAll(resolvedStarts)
        }
    }

    val sessionStatus by supabaseClient.auth.sessionStatus.collectAsState()
    val currentUser = remember(sessionStatus) {
        (sessionStatus as? io.github.jan.supabase.auth.status.SessionStatus.Authenticated)?.session?.user
    }

    fun uploadAttachment(attachment: AttachmentState) {
        val user = currentUser ?: return
        val conversationId = currentConversationId ?: "temp_conv"
        val path = "${user.id}/$conversationId/${attachment.fileName}"
        
        val index = selectedAttachments.indexOf(attachment)
        if (index != -1) {
            selectedAttachments[index] = attachment.copy(uploading = true)
        }
        
        coroutineScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(attachment.uri)?.use { it.readBytes() }
                if (bytes != null) {
                    supabaseClient.storage.from("attachments").upload(path, bytes) {
                        upsert = true
                    }
                    val idx = selectedAttachments.indexOfFirst { it.uri == attachment.uri }
                    if (idx != -1) {
                        selectedAttachments[idx] = selectedAttachments[idx].copy(
                            uploading = false,
                            uploadedPath = path
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val idx = selectedAttachments.indexOfFirst { it.uri == attachment.uri }
                if (idx != -1) {
                    selectedAttachments[idx] = selectedAttachments[idx].copy(uploading = false)
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (name, size) = getFileInfo(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val state = AttachmentState(uri, name, mimeType, size)
            selectedAttachments.add(state)
            uploadAttachment(state)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val (name, size) = getFileInfo(context, uri)
            val mimeType = context.contentResolver.getType(uri) ?: "application/pdf"
            val state = AttachmentState(uri, name, mimeType, size)
            selectedAttachments.add(state)
            uploadAttachment(state)
        }
    }

    var isVoiceInputActive by remember { mutableStateOf(false) }
    var isUsingGemmaVoice by remember { mutableStateOf(false) }
    var isTranscribing by remember { mutableStateOf(false) }
    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var rmsDbValue by remember { mutableStateOf(0f) }

    val audioRecorder = remember { com.kairos.os.domain.usecases.AudioRecorder() }
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.cancel()
            speechRecognizer.destroy()
        }
    }

    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                termInput = processVoiceText(text, availableApps)
            }
        }
        isVoiceInputActive = false
        isUsingGemmaVoice = false
        isTranscribing = false
        rmsDbValue = 0f
    }

    fun launchSystemSpeechFallback() {
        val fallbackIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        try {
            isVoiceInputActive = true
            systemSpeechLauncher.launch(fallbackIntent)
        } catch (e: Exception) {
            isVoiceInputActive = false
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context,
                    "Voice input is not supported on this device.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun startPlatformSpeechRecognition() {
        isUsingGemmaVoice = false
        isVoiceInputActive = true
        rmsDbValue = 0f
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                rmsDbValue = rmsdB
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                android.util.Log.e("Speech", "Speech recognizer error: $error")
                isVoiceInputActive = false
                rmsDbValue = 0f
                launchSystemSpeechFallback()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    termInput = processVoiceText(text, availableApps)
                }
                isVoiceInputActive = false
                rmsDbValue = 0f
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: ""
                if (text.isNotBlank()) {
                    termInput = processVoiceText(text, availableApps)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    fun startGemmaVoiceCapture() {
        isUsingGemmaVoice = true
        isVoiceInputActive = true
        rmsDbValue = 0f
        val started = audioRecorder.start(coroutineScope) { rmsDbValue = it }
        if (!started) {
            isUsingGemmaVoice = false
            isVoiceInputActive = false
            startPlatformSpeechRecognition()
        }
    }

    fun confirmVoiceInput() {
        if (isTranscribing) return
        if (isUsingGemmaVoice) {
            isTranscribing = true
            coroutineScope.launch {
                val wav = audioRecorder.stopAndGetWav()
                isVoiceInputActive = false
                isUsingGemmaVoice = false
                rmsDbValue = 0f
                val text = if (wav != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        gemmaSttClient.transcribe(wav)
                    }
                } else {
                    null
                }
                isTranscribing = false
                if (!text.isNullOrBlank()) {
                    termInput = processVoiceText(text, availableApps)
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "On-device transcription failed. Trying system voice input…",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    launchSystemSpeechFallback()
                }
            }
        } else {
            speechRecognizer.stopListening()
            isVoiceInputActive = false
            rmsDbValue = 0f
        }
    }

    fun cancelVoiceInput() {
        if (isTranscribing) return
        if (isUsingGemmaVoice) {
            audioRecorder.cancel()
            isUsingGemmaVoice = false
        } else {
            speechRecognizer.cancel()
        }
        isVoiceInputActive = false
        rmsDbValue = 0f
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingVoiceAction?.invoke()
        }
        pendingVoiceAction = null
    }

    fun onMicButtonClick(useGemma: Boolean) {
        pendingVoiceAction = if (useGemma) {
            { startGemmaVoiceCapture() }
        } else {
            { startPlatformSpeechRecognition() }
        }
        recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    val currentLineCount = textLayoutResult?.lineCount ?: 1
    if (currentLineCount > 1) {
        forceStackedLayout = true
    }
    if (termInput.trim().isEmpty()) {
        forceStackedLayout = false
    }

    val isStackedLayout = forceStackedLayout || selectedAttachments.isNotEmpty() || isVoiceInputActive

    LaunchedEffect(isStackedLayout) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // ignore
        }
    }
    
    fun isKaiApp(id: String?): Boolean {
        if (id == null) return false
        val clean = id.removePrefix("app:").lowercase()
        return clean == "kainotes" || clean == "notes" || clean == "kaicalendar" || clean == "calendar" || clean == "kaiclock" || clean == "clock" || clean == "alarm" || clean == "kaischeduled" || clean == "scheduled" || clean == "schedule"
    }

    val parsedActiveApp = remember(termInput, availableApps) {
        val cleanInput = termInput.trimStart()
        val firstWord = cleanInput.substringBefore(' ')
        if (firstWord.startsWith("@app:")) {
            val cleanSlug = firstWord.drop(1).removePrefix("app:").lowercase()
            val matchedApp = availableApps.find {
                it.id.equals("app:$cleanSlug", ignoreCase = true) ||
                it.id.removePrefix("app:").equals(cleanSlug, ignoreCase = true)
            }
            if (matchedApp != null && (matchedApp.packageName != null || matchedApp.category == "installed" || matchedApp.id.startsWith("app:"))) {
                matchedApp.id.lowercase()
            } else null
        } else if (firstWord.startsWith("@")) {
            val rawSlug = firstWord.drop(1).lowercase()
            val matchedKaiApp = availableApps.find {
                isKaiApp(it.id) && (
                    it.id.equals(rawSlug, ignoreCase = true) ||
                    it.id.equals("app:$rawSlug", ignoreCase = true) ||
                    it.id.removePrefix("app:").equals(rawSlug, ignoreCase = true)
                )
            }
            matchedKaiApp?.id?.lowercase()
        } else null
    }

    val parsedActiveIntegration = remember(termInput, availableApps) {
        val cleanInput = termInput.trimStart()
        val firstWord = cleanInput.substringBefore(' ')
        if (firstWord.startsWith("@") && !firstWord.startsWith("@app:")) {
            val rawSlug = firstWord.drop(1).lowercase()
            val matchedIntegration = availableApps.find { 
                it.id.equals(rawSlug, ignoreCase = true) && !it.id.startsWith("app:") && it.packageName == null
            }
            matchedIntegration?.id?.lowercase()
        } else null
    }

    val currentApp = remember(parsedActiveApp, availableApps) {
        if (parsedActiveApp != null) {
            availableApps.find { it.id.equals(parsedActiveApp, ignoreCase = true) }
        } else null
    }

    val isFrictionMode = remember(currentApp, distractingAppIds, activeSession) {
        val pkg = currentApp?.packageName
        val hasActiveGrant = pkg != null && appSessionManager.hasValidGrant(pkg)
        currentApp != null && !isKaiApp(currentApp.id) && intentViewModel.isDistractingApp(currentApp.id) && !hasActiveGrant
    }

    val frictionTargetApp = currentApp

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    LaunchedEffect(availableApps) {
        availableApps.forEach { app ->
            if (app.iconUrl != null && !iconCache.containsKey(app.id)) {
                val request = coil.request.ImageRequest.Builder(context)
                    .data(app.iconUrl)
                    .target { drawable ->
                        iconCache[app.id] = drawable
                    }
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    LaunchedEffect(termInput) {
        val cleanInput = termInput.trimStart()
        val atIndex = cleanInput.lastIndexOf('@')
        if (atIndex != -1) {
            val query = cleanInput.substring(atIndex + 1)
            if (!query.contains(" ")) {
                isAppDrawerOpen = true
                searchQuery = query.lowercase()
                if (query.startsWith("app:") || query.startsWith("app")) {
                    selectedDrawerTab = "App"
                }
            } else {
                isAppDrawerOpen = false
            }
        } else {
            isAppDrawerOpen = false
            searchQuery = ""
        }
    }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = termInput, selection = TextRange(termInput.length)))
    }

    LaunchedEffect(parsedActiveApp) {
        selectedFrictionTime = null
        frictionReason = ""
        intentApproved = false
        validationFeedback = null
    }

    val frictionTimeOptions = remember {
        listOf("5m" to 5, "10m" to 10, "15m" to 15, "30m" to 30, "45m" to 45, "1hr" to 60)
    }
    val remainingBudget = userSettings.remainingLeisureMinutes ?: userSettings.dailyLeisureMinutes
    val budgetExhausted = remainingBudget <= 0
    val selectedFrictionMinutes = frictionTimeOptions.find { it.first == selectedFrictionTime }?.second
    val isFrictionOpenAllowed = intentApproved && !isValidatingReason && selectedFrictionTime != null &&
        !budgetExhausted && selectedFrictionMinutes != null && selectedFrictionMinutes <= remainingBudget

    LaunchedEffect(isFrictionMode) {
        if (isFrictionMode) {
            intentViewModel.refreshSettings()
        }
    }

    LaunchedEffect(remainingBudget, selectedFrictionTime) {
        val selectedMinutes = frictionTimeOptions.find { it.first == selectedFrictionTime }?.second
        if (selectedMinutes != null && selectedMinutes > remainingBudget) {
            selectedFrictionTime = null
        }
    }

    LaunchedEffect(frictionReason, selectedFrictionTime) {
        if (isFrictionMode && selectedFrictionTime != null && frictionReason.trim().length >= 4) {
            delay(800)
            isValidatingReason = true
            try {
                val targetName = frictionTargetApp?.displayName ?: "App"
                val res = intentViewModel.validateReason(frictionReason, targetName)
                intentApproved = res.approved
                validationFeedback = if (!res.approved) res.feedback else null
            } catch (e: Exception) {
                intentApproved = true
                validationFeedback = null
            } finally {
                isValidatingReason = false
            }
        } else {
            intentApproved = false
            validationFeedback = null
        }
    }

    val launchDistractingApp: () -> Unit = launchDistractingApp@ {
        val app = frictionTargetApp
        val timeStr = selectedFrictionTime
        val pkg = app?.packageName
        if (app != null && pkg != null && appSessionManager.hasValidGrant(pkg)) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
            selectedFrictionTime = null
            frictionReason = ""
            intentApproved = false
            termInput = ""
            textFieldValue = androidx.compose.ui.text.input.TextFieldValue("")
            return@launchDistractingApp
        }
        if (app != null && timeStr != null && intentApproved) {
            val minutes = frictionTimeOptions.find { it.first == timeStr }?.second ?: 15
            if (minutes > remainingBudget) {
                validationFeedback = "Not enough leisure time remaining ($remainingBudget m left)."
                return@launchDistractingApp
            }
            coroutineScope.launch {
                val logRes = intentViewModel.logIntent(
                    appId = app.id,
                    displayName = app.displayName,
                    reason = frictionReason,
                    minutes = minutes,
                    aiApproved = intentApproved
                )
                if (logRes.budgetExceeded || !logRes.logged) {
                    validationFeedback = logRes.message ?: "Daily leisure limit reached (${logRes.remainingMinutes}m remaining)."
                } else {
                    val packageName = app.packageName
                    if (packageName != null) {
                        appSessionManager.startSession(
                            packageName = packageName,
                            displayName = app.displayName,
                            appSlug = app.id.removePrefix("app:").lowercase(),
                            minutes = minutes
                        )
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                    }
                    selectedFrictionTime = null
                    frictionReason = ""
                    intentApproved = false
                    termInput = ""
                    textFieldValue = androidx.compose.ui.text.input.TextFieldValue("")
                }
            }
        }
    }

    val interactions = remember { mutableStateListOf<com.kairos.os.domain.models.Interaction>() }
    var isLoading by remember { mutableStateOf(false) }

    fun openAgentConversation(id: String) {
        chatViewModel.selectConversation(id)
        val agent = runningAgents.find { it.id == id }
        interactions.clear()
        interactions.add(com.kairos.os.domain.models.Interaction.UserCommand(agent?.prompt ?: ""))
        if (agent?.response != null) {
            interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(agent.response))
            isLoading = false
        } else if (
            agent?.status == com.kairos.os.domain.models.AgentStatus.PROCESSING ||
            runningJobs.containsKey(id)
        ) {
            isLoading = true
            interactions.add(com.kairos.os.domain.models.Interaction.Loading())
        } else {
            isLoading = false
        }
        isChatOpen = true
    }

    LaunchedEffect(pendingAgentId, runningAgents) {
        val agentId = agentNotificationNavigationStore.consumePending() ?: return@LaunchedEffect
        isAgentsExpanded = false
        openAgentConversation(agentId)
    }

    val onSendPrompt = {
        if (termInput.isNotBlank()) {
            val currentIntent = termInput.trim()
            val currentTarget = parsedActiveIntegration ?: parsedActiveApp
            
            // Check for /open commands (Tier 0 direct launcher & local screens)
            if (currentIntent.startsWith("/open ")) {
                val appToOpen = currentIntent.substringAfter("/open ").trim().lowercase()
                if (appToOpen == "notes" || appToOpen == "kainotes") {
                    activeScreen = "notes"
                    termInput = ""
                    textFieldValue = TextFieldValue("")
                } else if (appToOpen == "calendar" || appToOpen == "kaicalendar") {
                    activeScreen = "calendar"
                    termInput = ""
                    textFieldValue = TextFieldValue("")
                } else if (appToOpen == "clock" || appToOpen == "kaiclock" || appToOpen == "alarm") {
                    activeScreen = "clock"
                    termInput = ""
                    textFieldValue = TextFieldValue("")
                } else {
                    val app = availableApps.find { it.id.removePrefix("app:").equals(appToOpen, ignoreCase = true) || it.displayName.lowercase() == appToOpen }
                    if (app?.packageName != null) {
                        try {
                            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                            if (intent != null) {
                                context.startActivity(intent)
                            }
                        } catch (e: Exception) {
                            Log.e("Launcher", "Failed to launch app: ${app.packageName}", e)
                        }
                    }
                    termInput = ""
                    textFieldValue = TextFieldValue("")
                }
            } else if (currentIntent == "@app:notes" || currentIntent == "@app:kainotes" || currentIntent == "@notes" || currentIntent == "@kainotes") {
                activeScreen = "notes"
                termInput = ""
                textFieldValue = TextFieldValue("")
            } else if (currentIntent == "@app:calendar" || currentIntent == "@app:kaicalendar" || currentIntent == "@calendar" || currentIntent == "@kaicalendar") {
                activeScreen = "calendar"
                termInput = ""
                textFieldValue = TextFieldValue("")
            } else if (currentIntent == "@app:clock" || currentIntent == "@app:kaiclock" || currentIntent == "@clock" || currentIntent == "@kaiclock" || currentIntent == "@alarm") {
                activeScreen = "clock"
                termInput = ""
                textFieldValue = TextFieldValue("")
            } else if (!isKaiApp(currentTarget) && currentIntent.startsWith("@app:")) {
                val appToOpen = currentIntent.substringAfter("@app:").trim().lowercase()
                val app = availableApps.find { it.id.equals("app:$appToOpen", ignoreCase = true) || it.id.removePrefix("app:").equals(appToOpen, ignoreCase = true) }
                if (app?.packageName != null) {
                    try {
                        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                        if (intent != null) {
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        Log.e("Launcher", "Failed to launch app: ${app.packageName}", e)
                    }
                }
                termInput = ""
                textFieldValue = TextFieldValue("")
            } else if (
                (currentIntent != "@$parsedActiveApp" && currentIntent != "@$parsedActiveIntegration")
                || parsedActiveIntegration == "digest"
            ) {
                val attachmentsPayload = selectedAttachments.mapNotNull { attachment ->
                    attachment.uploadedPath?.let { path ->
                        AttachmentInfo(
                            filePath = path,
                            fileName = attachment.fileName,
                            mimeType = attachment.mimeType,
                            fileSize = attachment.fileSize
                        )
                    }
                }
                val dispatchConvId = if (isChatOpen && currentConversationId != null) {
                    currentConversationId!!
                } else {
                    java.util.UUID.randomUUID().toString()
                }

                if (isChatOpen) {
                    interactions.add(com.kairos.os.domain.models.Interaction.UserCommand(currentIntent, currentTarget))
                    isLoading = true
                    interactions.add(com.kairos.os.domain.models.Interaction.Loading())
                }
                termInput = ""
                textFieldValue = TextFieldValue("")
                selectedAttachments.clear()
                focusManager.clearFocus()
                keyboardController?.hide()

                runningAgentsViewModel.dispatch(dispatchConvId, currentIntent, isLocal = true)

                val isDigest = currentTarget == "digest" || currentIntent.contains("@digest") || currentIntent.lowercase().trim() == "digest"

                val taskJob = coroutineScope.launch {
                    try {
                        if (isDigest) {
                            runningAgentsViewModel.updateStatusLine(dispatchConvId, "Composing digest…")
                            val response = localDigestGenerator.generateDigest()
                            runningAgentsViewModel.complete(dispatchConvId, response)
                            if (isChatOpen && currentConversationId == dispatchConvId) {
                                com.kairos.os.ui.utils.revealAssistantResponse(interactions, response)
                            }
                        } else {
                            val user = supabaseClient.auth.currentSessionOrNull()?.user
                            val userId = user?.id ?: "local_user"
                            val resolvedConvId = dispatchConvId

                            val onProgress: (String) -> Unit = { line ->
                                runningAgentsViewModel.updateStatusLine(dispatchConvId, line)
                            }

                            val localResponse = localAgentEngine.execute(
                                prompt = currentIntent,
                                appTarget = currentTarget,
                                conversationId = resolvedConvId,
                                userId = userId,
                                onProgress = onProgress
                            )

                            if (localResponse.type == "CLOUD_FALLBACK") {
                                Log.i("Launcher", "Local agent returned CLOUD_FALLBACK. Routing to Next.js backend...")

                                val targetConvId = dispatchConvId
                                var cloudConvId: String? = if (isChatOpen) currentConversationId else targetConvId
                                if (user != null) {
                                    try {
                                        supabaseClient.postgrest["conversations"].upsert(
                                            mapOf("id" to targetConvId, "user_id" to user.id, "title" to "New Conversation")
                                        )
                                        cloudConvId = targetConvId
                                    } catch (e: Exception) {
                                        Log.e("Launcher", "Failed to sync cloud conversation ID in Supabase", e)
                                    }
                                }

                                runningAgentsViewModel.updateStatusLine(dispatchConvId, "Connecting to cloud agent…")
                                com.kairos.os.ui.utils.cloudStatusFromPrompt(currentIntent)?.let {
                                    runningAgentsViewModel.updateStatusLine(dispatchConvId, it)
                                }

                                val rotatorJob = launch {
                                    val fallbacks = listOf("Thinking…", "Working on it…", "Almost there…")
                                    var index = 0
                                    while (true) {
                                        kotlinx.coroutines.delay(2500)
                                        runningAgentsViewModel.updateStatusLine(
                                            dispatchConvId,
                                            fallbacks[index++ % fallbacks.size]
                                        )
                                    }
                                }

                                runningAgentsViewModel.updateStatusLine(dispatchConvId, "Synthesizing answer…")
                                val response = try {
                                    apiClient.postPrompt(currentIntent, currentTarget, cloudConvId ?: targetConvId, attachmentsPayload)
                                } finally {
                                    rotatorJob.cancel()
                                }
                                val finalCloudConvId = response.meta?.conversationId ?: cloudConvId ?: targetConvId
                                chatViewModel.onPromptResponse(targetConvId)

                                launch {
                                    val genTitle = localTitleGenerator.generateAndSaveTitle(targetConvId, currentIntent, isLocal = false)
                                    if (genTitle.isNotBlank()) {
                                        runningAgentsViewModel.updateTitle(targetConvId, genTitle)
                                    }
                                    chatViewModel.onPromptResponse(targetConvId)
                                }

                                runningAgentsViewModel.complete(targetConvId, response)
                                if (isChatOpen && (currentConversationId == targetConvId || currentConversationId == finalCloudConvId)) {
                                    com.kairos.os.ui.utils.revealAssistantResponse(interactions, response)
                                }
                            } else {
                                chatViewModel.onPromptResponse(resolvedConvId)

                                launch {
                                    val genTitle = localTitleGenerator.generateAndSaveTitle(resolvedConvId, currentIntent, isLocal = true)
                                    if (genTitle.isNotBlank()) {
                                        runningAgentsViewModel.updateTitle(resolvedConvId, genTitle)
                                    }
                                    chatViewModel.onPromptResponse(resolvedConvId)
                                }
                                runningAgentsViewModel.complete(resolvedConvId, localResponse)
                                if (isChatOpen && currentConversationId == resolvedConvId) {
                                    com.kairos.os.ui.utils.revealAssistantResponse(interactions, localResponse)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) {
                            runningAgentsViewModel.cancel(dispatchConvId)
                            if (isChatOpen && currentConversationId == dispatchConvId) {
                                interactions.removeAll {
                                    it is com.kairos.os.domain.models.Interaction.Loading ||
                                        it is com.kairos.os.domain.models.Interaction.StreamingResponse
                                }
                            }
                        } else {
                            val errText = "Failed to process query: ${e.message}"
                            runningAgentsViewModel.markError(dispatchConvId, errText)
                            if (isChatOpen && currentConversationId == dispatchConvId) {
                                interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                                interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(
                                    com.kairos.os.domain.models.KairosResponse(
                                        type = "ERROR",
                                        text = errText
                                    )
                                ))
                            }
                        }
                    } finally {
                        runningJobs.remove(dispatchConvId)
                        if (isChatOpen && currentConversationId == dispatchConvId) {
                            isLoading = false
                        }
                    }
                }
                runningJobs[dispatchConvId] = taskJob
            }
        }
    }



    LaunchedEffect(termInput) {
        if (textFieldValue.text != termInput) {
            textFieldValue = TextFieldValue(text = termInput, selection = TextRange(termInput.length))
        }
    }

    val movableTextField = remember {
        movableContentOf { modifier: Modifier ->
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newVal ->
                    val oldVal = textFieldValue
                    val isDeletion = newVal.text.length < oldVal.text.length
                    var handled = false
                    
                    if (isDeletion) {
                        val deletedIndex = newVal.selection.start
                        val oldText = oldVal.text
                        val regex = Regex("@((?:app:)?[a-zA-Z0-9\\-]+)")
                        val match = regex.findAll(oldText).find { m ->
                            deletedIndex >= m.range.first && deletedIndex <= m.range.last
                        }
                        
                        if (match != null) {
                            val appId = match.groups[1]?.value?.lowercase() ?: ""
                            val isValidMention = availableApps.any { it.id.equals(appId, ignoreCase = true) || it.id.equals("app:$appId", ignoreCase = true) || it.id.removePrefix("app:").equals(appId, ignoreCase = true) }
                            if (isValidMention) {
                                val start = match.range.first
                                var end = match.range.last
                                if (end + 1 < oldText.length && oldText[end + 1] == ' ') {
                                    end += 1
                                }
                                val newText = (oldText.substring(0, start) + oldText.substring(end + 1)).trimStart()
                                val newSelection = TextRange(start.coerceAtMost(newText.length))
                                
                                textFieldValue = TextFieldValue(text = newText, selection = newSelection, composition = null)
                                termInput = newText
                                handled = true
                            }
                        }
                    }
                    
                    if (!handled) {
                        if (!isDeletion && parsedActiveApp != null && !isKaiApp(parsedActiveApp)) {
                            // Block typing additional prompt text after non-KAI installed app mentions
                        } else {
                            textFieldValue = newVal
                            termInput = newVal.text
                        }
                    }
                },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                modifier = modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { isTerminalFocused = it.isFocused }
                    .drawBehind {
                        drawMentionsPills(textLayoutResult, termInput, availableApps, validStartsState, iconCache)
                    },
                onTextLayout = { textLayoutResult = it },
                visualTransformation = mentionVisualTransformation,
                decorationBox = { innerTextField ->
                    if (termInput.isEmpty()) {
                        Text(
                            text = "Type your command",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = TextStyle(fontFamily = googleSansFont, fontSize = 14.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    onSendPrompt()
                })
            )
        }
    }

    LaunchedEffect(currentMessages, currentConversationId, runningAgents) {
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

            val convId = currentConversationId
            if (convId != null &&
                (runningJobs.containsKey(convId) ||
                    runningAgents.find { it.id == convId }?.status == com.kairos.os.domain.models.AgentStatus.PROCESSING)
            ) {
                isLoading = true
                if (interactions.none { it is com.kairos.os.domain.models.Interaction.Loading }) {
                    interactions.add(com.kairos.os.domain.models.Interaction.Loading())
                }
            } else if (convId != null &&
                interactions.none {
                    it is com.kairos.os.domain.models.Interaction.Loading ||
                        it is com.kairos.os.domain.models.Interaction.StreamingResponse
                }
            ) {
                isLoading = false
            }
        }
    }
    
    val screenOffset = if (isSidebarOpen) 280f else if (isSettingsOpen) -280f else 0f
    val animatedOffset by animateFloatAsState(targetValue = screenOffset, label = "ScreenOffset")

    Box(modifier = Modifier.fillMaxSize()) {
        // Blur Source Box (contains background Canvas and main scrollable content)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        ) {
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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = animatedOffset.dp)
                    .statusBarsPadding()
            ) {
                // Main Content
                when (activeScreen) {
                    "search" -> {
                        com.kairos.os.ui.screens.SearchChatScreen(
                            chatViewModel = chatViewModel,
                            onOpenConversation = { conversationId ->
                                chatViewModel.selectConversation(conversationId)
                                isChatOpen = true
                                activeScreen = "home"
                            }
                        )
                    }
                    "scheduled" -> {
                        com.kairos.os.ui.screens.ScheduledScreen(
                            scheduledViewModel = scheduledViewModel,
                            onOpenConversation = { conversationId ->
                                chatViewModel.selectConversation(conversationId)
                                isChatOpen = true
                                activeScreen = "home"
                            },
                            onBack = { activeScreen = "home" },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = onThemeToggle
                        )
                    }
                    "distracting_apps" -> {
                        com.kairos.os.ui.screens.DistractingAppsScreen(
                            intentViewModel = intentViewModel,
                            installedApps = installedApps,
                            onBack = { activeScreen = "home" },
                            onAdjustDailyLimit = { activeScreen = "leisure_limit" }
                        )
                    }
                    "leisure_limit" -> {
                        com.kairos.os.ui.screens.LeisureLimitScreen(
                            intentViewModel = intentViewModel,
                            onBack = { activeScreen = "distracting_apps" }
                        )
                    }
                    "notification_rules" -> {
                        com.kairos.os.ui.screens.NotificationRulesScreen(
                            intentViewModel = intentViewModel,
                            installedApps = installedApps,
                            onBack = { activeScreen = "home" }
                        )
                    }
                    "notes" -> {
                        com.kairos.os.ui.screens.LocalNotesScreen(
                            notesController = localNotesController,
                            onBack = { activeScreen = "home" },
                            onNoteEditorStateChanged = { isEditing, saveState, onSave, onCancel ->
                                noteIsEditing = isEditing
                                noteSaveState = saveState
                                noteSaveAction = onSave
                                noteCancelAction = onCancel
                            }
                        )
                    }
                    "calendar" -> {
                        com.kairos.os.ui.screens.LocalCalendarScreen(
                            calendarController = localCalendarController,
                            viewMode = calendarViewMode,
                            onBack = { activeScreen = "home" }
                        )
                    }
                    "clock" -> {
                        com.kairos.os.ui.screens.LocalClockScreen(
                            alarmController = localAlarmController,
                            onBack = { activeScreen = "home" }
                        )
                    }
                    else -> { // "home"
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
                            ChatView(
                                interactions = interactions,
                                availableApps = availableApps,
                                iconCache = iconCache,
                                statusLine = currentConversationId?.let { statusLines[it] }
                            )
                        }
                    }
                }
            }
        }

        // Overlay Box (sits on top, not captured by Haze, keeps Header and Input Box fully sharp)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = animatedOffset.dp)
                .statusBarsPadding()
                .imePadding()
        ) {
            if (isAgentsExpanded && homeActivityItems.isNotEmpty()) {
                com.kairos.os.ui.components.ExpandedAgentList(
                    items = homeActivityItems,
                    onCollapse = { isAgentsExpanded = false },
                    onViewAgent = { id -> openAgentConversation(id) },
                    onViewGrant = { session ->
                        context.packageManager.getLaunchIntentForPackage(session.packageName)?.apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }?.let { context.startActivity(it) }
                    },
                    onDismissAgent = { runningAgentsViewModel.cancel(it) },
                    onDismissGrant = { sessionCardHideStore.hide(it) },
                    hazeState = hazeState,
                    statusLines = statusLines
                )
            }
            // Header (Aligned to TopCenter, fading vertical gradient background)
            // ScheduledScreen renders its own top bar — skip the global header to avoid overlap.
            if (activeScreen != "scheduled") Box(
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
                    if (activeScreen != "home") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {
                                if (activeScreen == "notes" && noteIsEditing) {
                                    noteCancelAction?.invoke()
                                } else when (activeScreen) {
                                    "leisure_limit" -> activeScreen = "distracting_apps"
                                    else -> activeScreen = "home"
                                }
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                text = when (activeScreen) {
                                    "notes" -> "Kai Notes"
                                    "calendar" -> "Kai Calendar"
                                    "clock" -> "Kai Clock"
                                    "search" -> "Search"
                                    "distracting_apps" -> "Distracting Apps"
                                    "leisure_limit" -> "Daily Leisure Limit"
                                    "notification_rules" -> "Notification Rules"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
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
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (activeScreen == "notes" && noteIsEditing) {
                            IconButton(
                                onClick = {
                                    if (noteSaveState == com.kairos.os.ui.screens.NoteSaveState.ORANGE_SAVE) {
                                        noteSaveAction?.invoke()
                                    }
                                }
                            ) {
                                when (noteSaveState) {
                                    com.kairos.os.ui.screens.NoteSaveState.GRAY_CHECK -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Unmodified Note",
                                            tint = Color(0xFF888888)
                                        )
                                    }
                                    com.kairos.os.ui.screens.NoteSaveState.ORANGE_SAVE -> {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = "Save Note",
                                            tint = Color(0xFFFF6B00)
                                        )
                                    }
                                    com.kairos.os.ui.screens.NoteSaveState.ORANGE_CHECK -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Note Saved",
                                            tint = Color(0xFFFF6B00)
                                        )
                                    }
                                }
                            }
                        }
                        if (activeScreen == "calendar") {
                            IconButton(onClick = { calendarViewMode = if (calendarViewMode == "week") "month" else "week" }) {
                                Icon(
                                    imageVector = if (calendarViewMode == "week") Icons.Default.CalendarMonth else Icons.Default.ViewWeek,
                                    contentDescription = "Toggle Calendar View",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onThemeToggle) {
                            Icon(if (isDarkTheme) Icons.Default.Brightness4 else Icons.Default.Brightness7, contentDescription = "Toggle Theme", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (activeScreen == "home") {
                            IconButton(onClick = { isSettingsOpen = true }) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Input box column (Aligned to BottomCenter, fading bottom gradient background)
            if (activeScreen == "home" || activeScreen == "scheduled") {
                Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(24.dp)
            ) {
                if (!isChatOpen && homeActivityItems.isNotEmpty() && !isAgentsExpanded) {
                    com.kairos.os.ui.components.CollapsedAgentStack(
                        items = homeActivityItems.take(3),
                        totalCount = homeActivityItems.size,
                        onTapStack = { isAgentsExpanded = true },
                        onViewAgent = { id -> openAgentConversation(id) },
                        onViewGrant = { session ->
                            context.packageManager.getLaunchIntentForPackage(session.packageName)?.apply {
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }?.let { context.startActivity(it) }
                        },
                        onDismissAgent = { runningAgentsViewModel.cancel(it) },
                        onDismissGrant = { sessionCardHideStore.hide(it) },
                        statusLines = statusLines
                    )
                }

                AnimatedVisibility(visible = isAppDrawerOpen) {
                    val availableDrawerApps = if (activeScreen == "scheduled") {
                        localKaiApps + composioApps
                    } else {
                        currentTabApps
                    }
                    val filteredApps = availableDrawerApps.filter { 
                        it.id.contains(searchQuery, ignoreCase = true) || 
                        it.displayName.contains(searchQuery, ignoreCase = true) ||
                        it.id.removePrefix("app:").contains(searchQuery.removePrefix("app:"), ignoreCase = true)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(8.dp)
                    ) {
                        if (activeScreen != "scheduled") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("Integrations", "App").forEach { tab ->
                                    val isSelected = selectedDrawerTab == tab
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Color(0xFFFF6B00) else Color.Transparent)
                                            .clickable { selectedDrawerTab = tab }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tab,
                                            style = TextStyle(
                                                fontFamily = googleSansFont,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) Color.White else Color(0xFF9E9E9E)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(filteredApps.size) { index ->
                                val app = filteredApps[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val newText = insertAppMention(termInput, app.id)
                                            termInput = newText
                                            textFieldValue = androidx.compose.ui.text.input.TextFieldValue(text = newText, selection = androidx.compose.ui.text.TextRange(newText.length))
                                            isAppDrawerOpen = false
                                            searchQuery = ""
                                            focusRequester.requestFocus()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (app.iconEmoji != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(Color(0xFFFF6B00).copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(app.iconEmoji, fontSize = 16.sp)
                                        }
                                    } else {
                                        AsyncImage(
                                            model = app.iconDrawable ?: app.iconUrl,
                                            imageLoader = imageLoader,
                                            contentDescription = app.displayName,
                                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(app.displayName, style = TextStyle(fontFamily = googleSansFont, fontSize = 14.sp, fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
                                        Text("@${app.id}", style = TextStyle(fontFamily = googleSansFont, fontSize = 11.sp), color = Color(0xFF8A8A8A))
                                    }
                                }
                            }
                            if (filteredApps.isEmpty()) {
                                item {
                                    Text(
                                        "No ${selectedDrawerTab.lowercase()} connections found matching '@$searchQuery'", 
                                        style = MaterialTheme.typography.bodyMedium, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                var isPlusMenuOpen by remember { mutableStateOf(false) }
                val validStartsState = remember { mutableStateListOf<Int>() }
                val mentionVisualTransformation = remember(availableApps) {
                    MentionVisualTransformation(availableApps) { resolvedStarts ->
                        validStartsState.clear()
                        validStartsState.addAll(resolvedStarts)
                    }
                }

                AnimatedVisibility(visible = isPlusMenuOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlusMenuItem(
                            icon = Icons.Default.Apps,
                            label = "Add App",
                            onClick = {
                                isPlusMenuOpen = false
                                if (!termInput.endsWith("@")) {
                                    termInput = if (termInput.isEmpty() || termInput.endsWith(" ")) {
                                        termInput + "@"
                                    } else {
                                        termInput + " @"
                                    }
                                }
                                isAppDrawerOpen = true
                                searchQuery = ""
                            }
                        )
                        PlusMenuItem(
                            icon = Icons.Default.AttachFile,
                            label = "Add Files",
                            onClick = {
                                isPlusMenuOpen = false
                                val supportedMimeTypes = arrayOf(
                                    "application/pdf",
                                    "text/plain",
                                    "text/csv",
                                    "text/html",
                                    "text/rtf"
                                )
                                filePickerLauncher.launch(supportedMimeTypes)
                            }
                        )
                        PlusMenuItem(
                            icon = Icons.Default.Image,
                            label = "Add Images",
                            onClick = {
                                isPlusMenuOpen = false
                                imagePickerLauncher.launch("image/*")
                            }
                        )
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
                        val currentLineCount = textLayoutResult?.lineCount ?: 1
                        if (currentLineCount > 1) {
                            forceStackedLayout = true
                        }
                        if (termInput.trim().isEmpty()) {
                            forceStackedLayout = false
                        }

                        val isStackedLayout = forceStackedLayout || selectedAttachments.isNotEmpty() || isVoiceInputActive

                        if (isStackedLayout) {
                            if (selectedAttachments.isNotEmpty()) {
                                SelectedAttachmentsRow(
                                    attachments = selectedAttachments,
                                    onRemove = { selectedAttachments.remove(it) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Box(modifier = Modifier.fillMaxWidth().heightIn(min = 36.dp, max = 150.dp)) {
                                movableTextField(Modifier.fillMaxWidth().heightIn(min = 36.dp, max = 150.dp))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isVoiceInputActive) {
                                    IconButton(
                                        onClick = { cancelVoiceInput() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel Voice", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        WaveformView(rmsDb = rmsDbValue)
                                    }

                                    IconButton(
                                        onClick = { confirmVoiceInput() },
                                        enabled = !isTranscribing,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFFF6B00), CircleShape)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Confirm Voice", tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    IconButton(
                                        onClick = { isPlusMenuOpen = !isPlusMenuOpen },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlusMenuOpen) Icons.Default.Close else Icons.Default.Add,
                                            contentDescription = "Add Context",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    val activeTarget = parsedActiveIntegration ?: parsedActiveApp
                                    val isKaiTarget = isKaiApp(activeTarget)
                                    val promptAfterTag = termInput.substringAfter(' ').trim()
                                    val hasPromptAfterTag = termInput.contains(' ') && promptAfterTag.isNotEmpty()

                                    if (isFrictionMode) {
                                        val isCanOpen = isFrictionOpenAllowed
                                        IconButton(
                                            onClick = {
                                                if (isCanOpen) {
                                                    launchDistractingApp()
                                                }
                                            },
                                            enabled = isCanOpen,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.OpenInNew,
                                                contentDescription = "Open App",
                                                tint = if (isCanOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    } else if (isKaiTarget && !hasPromptAfterTag) {
                                        IconButton(
                                            onClick = {
                                                val clean = activeTarget?.removePrefix("app:")?.lowercase()
                                                if (clean == "notes" || clean == "kainotes") activeScreen = "notes"
                                                else if (clean == "calendar" || clean == "kaicalendar") activeScreen = "calendar"
                                                else if (clean == "clock" || clean == "kaiclock" || clean == "alarm") activeScreen = "clock"
                                                else if (clean == "scheduled" || clean == "kaischeduled" || clean == "schedule") activeScreen = "scheduled"
                                                termInput = ""
                                                textFieldValue = TextFieldValue("")
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = "Open App", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    } else if (!isKaiTarget && currentApp?.packageName != null) {
                                        IconButton(
                                            onClick = {
                                                val launchIntent = packageManager.getLaunchIntentForPackage(currentApp.packageName!!)
                                                if (launchIntent != null) {
                                                    context.startActivity(launchIntent)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = "Open App", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    } else {
                                        val isCurrentTaskRunning = isChatOpen && currentConversationId != null && (runningJobs.containsKey(currentConversationId) || runningAgents.find { it.id == currentConversationId }?.status == com.kairos.os.domain.models.AgentStatus.PROCESSING)
                                        if (isCurrentTaskRunning) {
                                            IconButton(
                                                onClick = {
                                                    val convId = currentConversationId
                                                    if (convId != null) {
                                                        runningJobs[convId]?.cancel()
                                                        runningJobs.remove(convId)
                                                        runningAgentsViewModel.cancel(convId)
                                                        interactions.removeAll {
                                                            it is com.kairos.os.domain.models.Interaction.Loading ||
                                                                it is com.kairos.os.domain.models.Interaction.StreamingResponse
                                                        }
                                                        isLoading = false
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Stop, contentDescription = "Stop Agent", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        val useGemma = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                            localLlmClient.isAudioReady()
                                                        }
                                                        onMicButtonClick(useGemma)
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            IconButton(
                                                onClick = {
                                                    onSendPrompt()
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { isPlusMenuOpen = !isPlusMenuOpen },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlusMenuOpen) Icons.Default.Close else Icons.Default.Add,
                                        contentDescription = "Add Context",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    movableTextField(Modifier.fillMaxWidth())
                                }

                                val activeTarget = parsedActiveIntegration ?: parsedActiveApp
                                val isKaiTarget = isKaiApp(activeTarget)
                                val promptAfterTag = termInput.substringAfter(' ').trim()
                                val hasPromptAfterTag = termInput.contains(' ') && promptAfterTag.isNotEmpty()
                                val currentApp = availableApps.find { it.id == parsedActiveApp } ?: frictionTargetApp

                                if (activeScreen == "scheduled") {
                                    // No send icon or open-in-new icon on scheduled screen - ACTIVATE on the config panel below is the CTA
                                } else if (isFrictionMode || (currentApp != null && intentViewModel.isDistractingApp(currentApp.id))) {
                                    val isCanOpen = isFrictionOpenAllowed
                                    IconButton(
                                        onClick = {
                                            if (isCanOpen) {
                                                launchDistractingApp()
                                            }
                                        },
                                        enabled = isCanOpen,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.OpenInNew,
                                            contentDescription = "Open App",
                                            tint = if (isCanOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                } else if (isKaiTarget && !hasPromptAfterTag) {
                                    IconButton(
                                        onClick = {
                                            val clean = activeTarget?.removePrefix("app:")?.lowercase()
                                            if (clean == "notes" || clean == "kainotes") activeScreen = "notes"
                                            else if (clean == "calendar" || clean == "kaicalendar") activeScreen = "calendar"
                                            else if (clean == "clock" || clean == "kaiclock" || clean == "alarm") activeScreen = "clock"
                                            else if (clean == "scheduled" || clean == "kaischeduled" || clean == "schedule") activeScreen = "scheduled"
                                            termInput = ""
                                            textFieldValue = TextFieldValue("")
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open App", tint = MaterialTheme.colorScheme.primary)
                                    }
                                } else if (!isKaiTarget && currentApp?.packageName != null) {
                                    IconButton(
                                        onClick = {
                                            val launchIntent = packageManager.getLaunchIntentForPackage(currentApp.packageName!!)
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open App", tint = MaterialTheme.colorScheme.primary)
                                    }
                                } else {
                                    val isCurrentTaskRunning = isChatOpen && currentConversationId != null && (runningJobs.containsKey(currentConversationId) || runningAgents.find { it.id == currentConversationId }?.status == com.kairos.os.domain.models.AgentStatus.PROCESSING)
                                    if (isCurrentTaskRunning) {
                                        IconButton(
                                            onClick = {
                                                val convId = currentConversationId
                                                if (convId != null) {
                                                    runningJobs[convId]?.cancel()
                                                    runningJobs.remove(convId)
                                                    runningAgentsViewModel.cancel(convId)
                                                    interactions.removeAll {
                                                        it is com.kairos.os.domain.models.Interaction.Loading ||
                                                            it is com.kairos.os.domain.models.Interaction.StreamingResponse
                                                    }
                                                    isLoading = false
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Stop, contentDescription = "Stop Agent", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        }
                                    } else {
                                        IconButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val useGemma = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                        localLlmClient.isAudioReady()
                                                    }
                                                    onMicButtonClick(useGemma)
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = {
                                                onSendPrompt()
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                         val hasSelectedAppMention = parsedActiveApp != null || parsedActiveIntegration != null
                         AnimatedVisibility(visible = activeScreen == "scheduled" && hasSelectedAppMention) {
                             com.kairos.os.ui.screens.ScheduleConfigBelowInputPanel(
                                 promptText = termInput.trim(),
                                 onActivate = { frequency, daysOfWeek, timeOfDay ->
                                     scheduledViewModel.createScheduledTask(
                                         prompt = termInput.trim(),
                                         appTarget = parsedActiveIntegration ?: parsedActiveApp,
                                         frequency = frequency,
                                         daysOfWeek = daysOfWeek,
                                         timeOfDay = timeOfDay,
                                         onSuccess = {
                                             termInput = ""
                                             textFieldValue = androidx.compose.ui.text.input.TextFieldValue("")
                                             scheduledViewModel.refreshAll()
                                         }
                                     )
                                 }
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
                                         withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                             append("@${frictionTargetApp?.displayName ?: parsedActiveApp ?: "app"}.")
                                         }
                                     },
                                     style = MaterialTheme.typography.bodyMedium,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                                 Spacer(modifier = Modifier.height(16.dp))
                                 
                                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                     frictionTimeOptions.forEach { (time, minutes) ->
                                         val isSelected = selectedFrictionTime == time
                                         val isChipEnabled = minutes <= remainingBudget
                                         Box(
                                             modifier = Modifier
                                                 .weight(1f)
                                                 .background(
                                                     if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background,
                                                     RoundedCornerShape(8.dp)
                                                 )
                                                 .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                                 .clickable(enabled = isChipEnabled) {
                                                     if (isChipEnabled) selectedFrictionTime = time
                                                 }
                                                 .padding(vertical = 10.dp),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             Text(
                                                 time,
                                                 color = when {
                                                     isSelected -> MaterialTheme.colorScheme.primary
                                                     isChipEnabled -> MaterialTheme.colorScheme.onBackground
                                                     else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                 },
                                                 style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                             )
                                         }
                                     }
                                 }
                                 if (budgetExhausted) {
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Text(
                                         text = "Daily leisure budget used up.",
                                         style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                                         color = MaterialTheme.colorScheme.error
                                     )
                                 }
                                 Spacer(modifier = Modifier.height(16.dp))
                                 
                                 BasicTextField(
                                     value = frictionReason,
                                     onValueChange = { if (it.length <= 80) frictionReason = it },
                                     textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontFamily = googleSansFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
                                     cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                         .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                         .padding(12.dp),
                                     decorationBox = { innerTextField ->
                                         if (frictionReason.isEmpty()) {
                                             Text("[reason] (e.g. reply to DM)", color = MaterialTheme.colorScheme.onSurfaceVariant, style = TextStyle(fontFamily = googleSansFont, fontSize = 14.sp))
                                         }
                                         innerTextField()
                                     }
                                 )
                                 
                                 Spacer(modifier = Modifier.height(4.dp))
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Text(
                                         text = "${remainingBudget}m leisure left today",
                                         style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont, fontWeight = FontWeight.SemiBold),
                                         color = if (budgetExhausted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                     )
                                     Text(
                                         text = "${frictionReason.length}/80",
                                         style = MaterialTheme.typography.labelSmall.copy(fontFamily = googleSansFont),
                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                     )
                                 }
                                 
                                 if (isValidatingReason) {
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Text(
                                         text = "Validating intent locally...",
                                         style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                                         color = MaterialTheme.colorScheme.primary
                                     )
                                 } else if (validationFeedback != null) {
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Text(
                                         text = validationFeedback!!,
                                         style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                                         color = MaterialTheme.colorScheme.error
                                     )
                                 }
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
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.90f))
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            drawLine(
                                color = surfaceVariantColor,
                                start = androidx.compose.ui.geometry.Offset(size.width - strokeWidth / 2, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width - strokeWidth / 2, size.height),
                                strokeWidth = strokeWidth
                            )
                        }
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                ) {
                    val logoPath = if (isDarkTheme) "file:///android_asset/logomark-for-dark.svg" else "file:///android_asset/logomark-for-light.svg"
                    val wordmarkPath = if (isDarkTheme) "file:///android_asset/wordmark-for-dark.svg" else "file:///android_asset/wordmark-for-light.svg"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        AsyncImage(
                            model = logoPath,
                            contentDescription = "KaiOS Logo",
                            imageLoader = imageLoader,
                            modifier = Modifier.size(24.dp)
                        )
                        AsyncImage(
                            model = wordmarkPath,
                            contentDescription = "KaiOS Wordmark",
                            imageLoader = imageLoader,
                            modifier = Modifier.height(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Ghost Buttons
                    SidebarButton(
                        icon = Icons.Default.Add,
                        text = "New chat",
                        isSelected = activeScreen == "home" && !isChatOpen,
                        onClick = {
                            isSidebarOpen = false
                            activeScreen = "home"
                            isChatOpen = false
                            chatViewModel.startNewConversation()
                            interactions.clear()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SidebarButton(
                        icon = Icons.Default.Search,
                        text = "Search Chat",
                        isSelected = activeScreen == "search",
                        onClick = {
                            isSidebarOpen = false
                            activeScreen = "search"
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SidebarButton(
                        icon = Icons.Default.Schedule,
                        text = "Scheduled",
                        isSelected = activeScreen == "scheduled",
                        onClick = {
                            isSidebarOpen = false
                            activeScreen = "scheduled"
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // History subheading
                    Text(
                        text = "HISTORY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val visibleConversations = conversations.filter { it.id !in deletedConversationIds }
                    androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(visibleConversations.size) { index ->
                            val conv = visibleConversations[index]
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                isSidebarOpen = false
                                                chatViewModel.selectConversation(conv.id)
                                                isChatOpen = true
                                                activeScreen = "home"
                                            },
                                            onLongClick = {
                                                showMenu = true
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 16.dp)
                                ) {
                                    val rawTitle = conv.title ?: "New Conversation"
                                    val displayTitle = if (conv.source == "scheduled" || conv.scheduledTaskId != null) {
                                        if (rawTitle.startsWith("🔄")) rawTitle else "🔄 $rawTitle"
                                    } else {
                                        rawTitle
                                    }
                                    Text(displayTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val displayDate = conv.createdAt.take(10)
                                    Text(displayDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Open", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont)) },
                                        onClick = {
                                            showMenu = false
                                            isSidebarOpen = false
                                            chatViewModel.selectConversation(conv.id)
                                            isChatOpen = true
                                            activeScreen = "home"
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont)) },
                                        onClick = {
                                            showMenu = false
                                            deletedConversationIds.add(conv.id)
                                            if (currentConversationId == conv.id) {
                                                isChatOpen = false
                                                chatViewModel.startNewConversation()
                                                interactions.clear()
                                            }
                                        }
                                    )
                                }
                            }
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
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = googleSansFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Distracting Apps Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isSettingsOpen = false
                                activeScreen = "distracting_apps"
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Distracting Apps",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Distracting Apps",
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Leisure budget & friction gates",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))

                    Spacer(modifier = Modifier.height(8.dp))

                    // Notification Rules Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isSettingsOpen = false
                                activeScreen = "notification_rules"
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notification Rules",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Rules",
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "App notification filtering rules",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))

                    Spacer(modifier = Modifier.height(8.dp))

                    // System Settings Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                isSettingsOpen = false
                                val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                                context.startActivity(intent)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "System Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "System Settings",
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = googleSansFont, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Device & OS configuration",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = googleSansFont),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(
                            text = "LOGOUT",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = googleSansFont,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
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

class MentionOffsetMapping(
    val originalText: String,
    val starts: List<Int>
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int {
        var insertions = 0
        for (start in starts) {
            if (offset > start) {
                insertions++
            }
        }
        return offset + insertions
    }

    override fun transformedToOriginal(offset: Int): Int {
        var insertions = 0
        for (start in starts) {
            if (offset > start + insertions) {
                insertions++
            }
        }
        return offset - insertions
    }
}

class MentionVisualTransformation(
    val availableApps: List<AppConnection>,
    val onStartsResolved: (List<Int>) -> Unit
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val regex = Regex("@((?:app:)?[a-zA-Z0-9\\-]+)")
        val matches = regex.findAll(originalText).toList()
        
        val validStarts = mutableListOf<Int>()
        val builder = AnnotatedString.Builder()
        
        var lastOffset = 0
        matches.forEach { match ->
            val appId = match.groups[1]?.value?.lowercase() ?: ""
            if (availableApps.any { it.id == appId }) {
                validStarts.add(match.range.first)
                
                builder.append(originalText.substring(lastOffset, match.range.first))
                
                val spaceStart = builder.length
                builder.append("\u00A0")
                builder.addStyle(
                    SpanStyle(
                        color = Color.Transparent,
                        letterSpacing = 16.sp
                    ),
                    spaceStart,
                    spaceStart + 1
                )
                
                val mentionStart = builder.length
                builder.append(match.value)
                builder.addStyle(
                    SpanStyle(
                        color = Color(0xFFFF6B00),
                        fontWeight = FontWeight.Bold
                    ),
                    mentionStart,
                    builder.length
                )
            } else {
                builder.append(originalText.substring(lastOffset, match.range.last + 1))
            }
            lastOffset = match.range.last + 1
        }
        if (lastOffset < originalText.length) {
            builder.append(originalText.substring(lastOffset))
        }
        
        onStartsResolved(validStarts)
        return TransformedText(builder.toAnnotatedString(), MentionOffsetMapping(originalText, validStarts))
    }
}

@Composable
fun WaveformView(rmsDb: Float) {
    val barCount = 20
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val normalizedDb = ((rmsDb + 2f).coerceAtLeast(0f) / 10f).coerceIn(0f, 1f)
        val baseHeight = 6.dp
        val maxHeight = 36.dp

        for (i in 0 until barCount) {
            val centerFactor = 1f - (Math.abs(i - barCount / 2).toFloat() / (barCount / 2))
            val waveAmplitude = Math.sin((phase + i * 0.5f).toDouble()).toFloat() * 0.3f + 0.7f
            val targetHeight = baseHeight + (maxHeight - baseHeight) * normalizedDb * centerFactor * waveAmplitude

            val idleHeight = baseHeight + (Math.sin((phase + i * 0.3f).toDouble()).toFloat() * 3f).dp
            val animatedHeight by animateDpAsState(
                targetValue = if (normalizedDb > 0.05f) targetHeight else idleHeight,
                label = "bar_height_$i"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(3.dp)
                    .height(animatedHeight)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}

@Composable
fun SelectedAttachmentsRow(
    attachments: List<AttachmentState>,
    onRemove: (AttachmentState) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(attachments) { attachment ->
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (attachment.mimeType.startsWith("image/")) Icons.Default.Image else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 120.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                if (attachment.uploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onRemove(attachment) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlusMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ChatView(
    interactions: MutableList<com.kairos.os.domain.models.Interaction>,
    availableApps: List<AppConnection>,
    iconCache: Map<String, android.graphics.drawable.Drawable>,
    statusLine: String? = null
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lastInteractionKey = interactions.lastOrNull()?.let { interaction ->
        when (interaction) {
            is com.kairos.os.domain.models.Interaction.StreamingResponse -> interaction.text
            else -> interaction.toString()
        }
    }

    LaunchedEffect(interactions.size, lastInteractionKey, statusLine) {
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
                    val showPrefix = if (interaction.appTarget != null && !interaction.command.contains("@${interaction.appTarget}")) {
                        "@${interaction.appTarget} "
                    } else ""
                    ChatBubble(
                        isUser = true, 
                        text = "$showPrefix${interaction.command}",
                        availableApps = availableApps,
                        iconCache = iconCache
                    )
                }
                is com.kairos.os.domain.models.Interaction.AssistantResponse -> {
                    if (!interaction.response.text.isNullOrBlank()) {
                        ChatBubble(
                            isUser = false, 
                            text = interaction.response.text,
                            availableApps = availableApps,
                            iconCache = iconCache,
                            modelName = interaction.response.meta?.model
                        )
                    }
                    if (interaction.response.widget != null) {
                        if (!interaction.response.text.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        var associatedAppTarget: String? = null
                        val currentIndex = interactions.indexOf(interaction)
                        if (currentIndex != -1) {
                            for (i in (currentIndex - 1) downTo 0) {
                                val prev = interactions[i]
                                if (prev is com.kairos.os.domain.models.Interaction.UserCommand) {
                                    associatedAppTarget = prev.appTarget
                                    break
                                }
                            }
                        }

                        com.kairos.os.ui.components.WidgetRenderer(
                            widget = interaction.response.widget,
                            appTarget = associatedAppTarget,
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
                is com.kairos.os.domain.models.Interaction.StreamingResponse -> {
                    ChatBubble(
                        isUser = false,
                        text = interaction.text,
                        availableApps = availableApps,
                        iconCache = iconCache,
                        modelName = interaction.modelName,
                        showStreamingCursor = !interaction.isComplete
                    )
                }
                is com.kairos.os.domain.models.Interaction.Loading -> {
                    com.kairos.os.ui.components.AgentThinkingIndicator(statusLine = statusLine)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
        
        Spacer(modifier = Modifier.height(130.dp))
    }
}

@Composable
fun ChatBubble(
    isUser: Boolean, 
    text: String,
    availableApps: List<AppConnection>,
    iconCache: Map<String, android.graphics.drawable.Drawable>,
    modelName: String? = null,
    showStreamingCursor: Boolean = false
) {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val codeText = MaterialTheme.colorScheme.primary
    val baseAnnotated = remember(text, codeBg, codeText) {
        if (isUser) {
            AnnotatedString(text)
        } else {
            parseMarkdownToAnnotatedString(text, codeBg, codeText)
        }
    }
    
    val inlineContentMap = remember(text, availableApps, iconCache) {
        val map = mutableMapOf<String, InlineTextContent>()
        val regex = Regex("@([a-zA-Z0-9\\-]+)")
        val matches = regex.findAll(baseAnnotated.text).toList()
        matches.forEach { match ->
            val appId = match.groups[1]?.value?.lowercase() ?: ""
            val app = availableApps.find { it.id == appId }
            if (app != null) {
                val inlineId = "app_logo_${app.id}"
                if (!map.containsKey(inlineId)) {
                    map[inlineId] = InlineTextContent(
                        Placeholder(
                            width = 20.sp,
                            height = 20.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        val drawable = app.iconDrawable ?: iconCache[app.id]
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (drawable != null) {
                                val bitmap = remember(drawable) {
                                    val bmp = android.graphics.Bitmap.createBitmap(
                                        drawable.intrinsicWidth.coerceAtLeast(1),
                                        drawable.intrinsicHeight.coerceAtLeast(1),
                                        android.graphics.Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = android.graphics.Canvas(bmp)
                                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                                    drawable.draw(canvas)
                                    bmp
                                }
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = app.displayName,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (app.iconUrl != null) {
                                AsyncImage(
                                    model = app.iconUrl,
                                    contentDescription = app.displayName,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
        map
    }

    val annotatedText = remember(text, baseAnnotated, availableApps) {
        buildAnnotatedString {
            val regex = Regex("@([a-zA-Z0-9\\-]+)")
            val matches = regex.findAll(baseAnnotated.text).toList()
            
            var lastOffset = 0
            matches.forEach { match ->
                val appId = match.groups[1]?.value?.lowercase() ?: ""
                val app = availableApps.find { it.id == appId }
                if (app != null) {
                    append(baseAnnotated.subSequence(lastOffset, match.range.first))
                    appendInlineContent("app_logo_${app.id}", "[logo]")
                    val start = length
                    append(match.value)
                    addStyle(
                        SpanStyle(
                            color = Color(0xFFFF6B00),
                            fontWeight = FontWeight.Bold
                        ),
                        start,
                        length
                    )
                } else {
                    append(baseAnnotated.subSequence(lastOffset, match.range.last + 1))
                }
                lastOffset = match.range.last + 1
            }
            if (lastOffset < baseAnnotated.length) {
                append(baseAnnotated.subSequence(lastOffset, baseAnnotated.length))
            }
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val maxBubbleWidth = (configuration.screenWidthDp * 0.6f).dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (isUser) Modifier.widthIn(max = maxBubbleWidth) else Modifier
                )
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
                    start = if (isUser) 20.dp else 0.dp,
                    end = if (isUser) 20.dp else 0.dp,
                    top = if (isUser) 16.dp else 12.dp,
                    bottom = if (isUser) 16.dp else 12.dp
                )
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = annotatedText,
                    inlineContent = inlineContentMap,
                    color = if (isUser) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp,
                        letterSpacing = 0.25.sp
                    )
                )
                if (showStreamingCursor) {
                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "cursor")
                    val cursorAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(500),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "cursorAlpha"
                    )
                    Text(
                        text = "|",
                        color = MaterialTheme.colorScheme.primary.copy(alpha = cursorAlpha),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            lineHeight = 24.sp
                        )
                    )
                }
            }
        }

        if (!isUser && !modelName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = if (modelName.contains("Local")) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFF6A1B9A).copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp, 
                    if (modelName.contains("Local")) Color(0xFF4CAF50) else Color(0xFFAB47BC)
                ),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (modelName.contains("Local")) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                        contentDescription = null,
                        tint = if (modelName.contains("Local")) Color(0xFF4CAF50) else Color(0xFFAB47BC),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = modelName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (modelName.contains("Local")) Color(0xFF4CAF50) else Color(0xFFAB47BC)
                    )
                }
            }
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

@Composable
fun SidebarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = googleSansFont,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ScheduledDummyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Schedule,
            contentDescription = "Scheduled",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Scheduled Tasks & Flows",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = googleSansFont),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dummy Page — Scheduled triggers and automations will live here.",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

