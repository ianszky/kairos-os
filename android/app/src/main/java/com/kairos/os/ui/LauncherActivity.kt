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
                            apiClient = apiClient,
                            supabaseClient = supabaseClient,
                            localDigestGenerator = localDigestGenerator,
                            localAgentEngine = localAgentEngine,
                            localTitleGenerator = localTitleGenerator,
                            localNotesController = localNotesController,
                            localCalendarController = localCalendarController,
                            localAlarmController = localAlarmController
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

val localKaiApps = listOf(
    AppConnection("kai", "Kai AI Agent", null, null, "local"),
    AppConnection("kainotes", "Kai Notes", null, null, "local"),
    AppConnection("kaicalendar", "Kai Calendar", null, null, "local"),
    AppConnection("kaiclock", "Kai Clock", null, null, "local")
)

val composioApps = listOf(
    AppConnection("digest", "Digest Summary", null, null, "utility"),
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
    val regex = Regex("@([a-zA-Z0-9\\-]+)")
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
    localAlarmController: com.kairos.os.domain.tools.LocalAlarmController
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
    var activeScreen by remember { mutableStateOf("home") }
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
                id = appName.lowercase().replace(" ", "-"),
                displayName = appName,
                iconDrawable = icon,
                category = "installed",
                packageName = packageName
            )
        }.sortedBy { it.displayName }
    }

    val availableApps = remember { (localKaiApps + composioApps + installedApps) }
    
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
    var rmsDbValue by remember { mutableStateOf(0f) }
    var speechTextResult by remember { mutableStateOf("") }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
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
        rmsDbValue = 0f
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isVoiceInputActive = true
            rmsDbValue = 0f
            speechTextResult = ""
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
                    if (error == SpeechRecognizer.ERROR_CLIENT) {
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                "No voice recognition service is selected in device settings.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val fallbackIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        }
                        try {
                            systemSpeechLauncher.launch(fallbackIntent)
                        } catch (e: Exception) {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Voice input is not supported on this device.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
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
    }
    
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

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = termInput, selection = TextRange(termInput.length)))
    }

    val interactions = remember { mutableStateListOf<com.kairos.os.domain.models.Interaction>() }
    var isLoading by remember { mutableStateOf(false) }

    val onSendPrompt = {
        if (termInput.isNotBlank()) {
            val currentIntent = termInput.trim()
            val currentTarget = parsedActiveApp
            
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
                    val app = availableApps.find { it.id.equals(appToOpen, ignoreCase = true) || it.displayName.lowercase() == appToOpen }
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
            } else if (currentIntent == "@notes" || currentIntent == "@kainotes") {
                activeScreen = "notes"
                termInput = ""
                textFieldValue = TextFieldValue("")
            } else if (currentIntent == "@calendar" || currentIntent == "@kaicalendar") {
                activeScreen = "calendar"
                termInput = ""
                textFieldValue = TextFieldValue("")
            } else if (currentIntent == "@clock" || currentIntent == "@kaiclock" || currentIntent == "@alarm") {
                activeScreen = "clock"
                termInput = ""
                textFieldValue = TextFieldValue("")
            } else if (currentIntent != "@$parsedActiveApp") {
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
                isChatOpen = true
                interactions.add(com.kairos.os.domain.models.Interaction.UserCommand(currentIntent, currentTarget))
                isLoading = true
                interactions.add(com.kairos.os.domain.models.Interaction.Loading())
                termInput = ""
                selectedAttachments.clear()

                val isDigest = currentTarget == "digest" || currentIntent.contains("@digest") || currentIntent.lowercase().trim() == "digest"

                if (isDigest) {
                    coroutineScope.launch {
                        try {
                            val response = localDigestGenerator.generateDigest()
                            interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                            interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(response))
                        } catch (e: Exception) {
                            interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                            interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(
                                com.kairos.os.domain.models.KairosResponse(
                                    type = "ERROR",
                                    text = "Failed to compile local digest: ${e.message}"
                                )
                            ))
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    coroutineScope.launch {
                        try {
                            var activeConvId = currentConversationId
                            val user = supabaseClient.auth.currentSessionOrNull()?.user
                            if (activeConvId == null && user != null) {
                                try {
                                    val newConv = supabaseClient.postgrest["conversations"].insert(
                                        mapOf("user_id" to user.id, "title" to "New Conversation")
                                    ) {
                                        select()
                                    }.decodeSingle<com.kairos.os.domain.models.Conversation>()
                                    activeConvId = newConv.id
                                    chatViewModel.onPromptResponse(activeConvId)
                                    val finalConvId = activeConvId
                                    if (finalConvId != null) {
                                        launch {
                                            localTitleGenerator.generateAndSaveTitle(finalConvId, currentIntent)
                                            chatViewModel.onPromptResponse(finalConvId)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("Launcher", "Failed to create conversation locally in Supabase", e)
                                }
                            }

                            val resolvedConvId = activeConvId ?: java.util.UUID.randomUUID().toString()
                            val userId = user?.id ?: ""

                            val localResponse = localAgentEngine.execute(
                                prompt = currentIntent,
                                appTarget = currentTarget,
                                conversationId = resolvedConvId,
                                userId = userId
                            )

                            if (localResponse.type == "CLOUD_FALLBACK") {
                                Log.i("Launcher", "Local agent returned CLOUD_FALLBACK. Routing to Next.js backend...")
                                val response = apiClient.postPrompt(currentIntent, currentTarget, resolvedConvId, attachmentsPayload)
                                chatViewModel.onPromptResponse(response.meta?.conversationId)
                                interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                                interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(response))
                            } else {
                                chatViewModel.onPromptResponse(resolvedConvId)
                                interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                                interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(localResponse))
                            }
                        } catch (e: Exception) {
                            interactions.removeAll { it is com.kairos.os.domain.models.Interaction.Loading }
                            interactions.add(com.kairos.os.domain.models.Interaction.AssistantResponse(
                                com.kairos.os.domain.models.KairosResponse(
                                    type = "ERROR",
                                    text = "Failed to process query: ${e.message}"
                                )
                            ))
                        } finally {
                            isLoading = false
                        }
                    }
                }
            }
        }
    }



    LaunchedEffect(termInput) {
        val firstWord = termInput.substringBefore(' ')
        val activeApp = if (firstWord.startsWith("@")) {
            val slug = firstWord.drop(1)
            val app = availableApps.find { it.id.equals(slug, ignoreCase = true) }
            if (app != null && app.category == "installed") app else null
        } else null
        
        val resolvedText = if (activeApp != null) {
            "@${activeApp.id}"
        } else {
            termInput
        }
        
        if (textFieldValue.text != resolvedText) {
            textFieldValue = TextFieldValue(text = resolvedText, selection = TextRange(resolvedText.length))
        }
        if (termInput != resolvedText) {
            termInput = resolvedText
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
                        val regex = Regex("@([a-zA-Z0-9\\-]+)")
                        val match = regex.findAll(oldText).find { m ->
                            deletedIndex >= m.range.first && deletedIndex <= m.range.last + 1
                        }
                        
                        if (match != null) {
                            val start = match.range.first
                            val end = match.range.last + 1
                            val newText = oldText.substring(0, start) + oldText.substring(end)
                            val newSelection = TextRange(start)
                            
                            textFieldValue = TextFieldValue(text = newText, selection = newSelection)
                            termInput = newText
                            handled = true
                        }
                    }
                    
                    if (!handled) {
                        val firstWordOld = oldVal.text.substringBefore(' ')
                        val activeAppOld = if (firstWordOld.startsWith("@")) {
                            val slug = firstWordOld.drop(1)
                            val app = availableApps.find { it.id.equals(slug, ignoreCase = true) }
                            if (app != null && app.category == "installed") app else null
                        } else null
                        
                        if (activeAppOld != null) {
                            val expectedText = "@${activeAppOld.id}"
                            val expectedTextWithSpace = "@${activeAppOld.id} "
                            if (newVal.text != expectedText && newVal.text != expectedTextWithSpace) {
                                handled = true
                            }
                        }
                    }
                    
                    if (!handled) {
                        val firstWordNew = newVal.text.substringBefore(' ')
                        val activeAppNew = if (firstWordNew.startsWith("@")) {
                            val slug = firstWordNew.drop(1)
                            val app = availableApps.find { it.id.equals(slug, ignoreCase = true) }
                            if (app != null && app.category == "installed") app else null
                        } else null
                        
                        if (activeAppNew != null) {
                            val formattedText = "@${activeAppNew.id}"
                            if (newVal.text != formattedText && newVal.text != "$formattedText ") {
                                val resolvedVal = TextFieldValue(
                                    text = formattedText,
                                    selection = TextRange(formattedText.length)
                                )
                                textFieldValue = resolvedVal
                                termInput = formattedText
                                handled = true
                            }
                        }
                    }
                    
                    if (!handled) {
                        textFieldValue = newVal
                        termInput = newVal.text
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
                        SearchDummyScreen()
                    }
                    "scheduled" -> {
                        ScheduledDummyScreen()
                    }
                    "notes" -> {
                        com.kairos.os.ui.screens.LocalNotesScreen(
                            notesController = localNotesController,
                            onBack = { activeScreen = "home" }
                        )
                    }
                    "calendar" -> {
                        com.kairos.os.ui.screens.LocalCalendarScreen(
                            calendarController = localCalendarController,
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
                                iconCache = iconCache
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
                    if (activeScreen != "home") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { activeScreen = "home" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back to Home", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Text(
                                text = when (activeScreen) {
                                    "notes" -> "Kai Notes"
                                    "calendar" -> "Kai Calendar"
                                    "clock" -> "Kai Clock"
                                    "search" -> "Search"
                                    "scheduled" -> "Scheduled Tasks"
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
            if (activeScreen == "home") {
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
                                            termInput = insertAppMention(termInput, app.id)
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
                                        Text(if (app.packageName != null) "App" else if (app.category == "local") "Local App" else "Integration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                                        onClick = {
                                            speechRecognizer.cancel()
                                            isVoiceInputActive = false
                                            rmsDbValue = 0f
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Cancel Voice", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        WaveformView(rmsDb = rmsDbValue)
                                    }

                                    IconButton(
                                        onClick = {
                                            speechRecognizer.stopListening()
                                            isVoiceInputActive = false
                                            rmsDbValue = 0f
                                        },
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

                                    val currentApp = availableApps.find { it.id == parsedActiveApp }
                                    if (currentApp?.packageName != null) {
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
                                        IconButton(
                                            onClick = {
                                                recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
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

                                val currentApp = availableApps.find { it.id == parsedActiveApp }
                                if (currentApp?.packageName != null) {
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
                                    IconButton(
                                        onClick = {
                                            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
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
                                    val displayTitle = conv.title ?: "New Conversation"
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
        val regex = Regex("@([a-zA-Z0-9\\-]+)")
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
    iconCache: Map<String, android.graphics.drawable.Drawable>
) {
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
                is com.kairos.os.domain.models.Interaction.Loading -> {
                    com.kairos.os.ui.components.TypingIndicator()
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
    modelName: String? = null
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
fun SearchDummyScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Search Chat Histories",
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = googleSansFont),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dummy Page — Search UI will be integrated here.",
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = googleSansFont),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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

