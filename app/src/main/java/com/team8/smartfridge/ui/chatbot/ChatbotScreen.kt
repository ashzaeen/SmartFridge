package com.team8.smartfridge.ui.chatbot

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.team8.smartfridge.data.FirebaseRepository
import com.team8.smartfridge.model.PantryItem
import com.team8.smartfridge.model.User
import com.team8.smartfridge.network.OpenAIClient
import com.team8.smartfridge.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Data ──────────────────────────────────────────────────────────────────────

enum class MessageRole { USER, AI }

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val role: MessageRole,
    val text: String,
    val isLoading: Boolean = false
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ChatbotViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FirebaseRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val messageHistory = mutableListOf<OpenAIClient.Message>()
    private var initializedWithUser = false
    private var initializedOnce = false

    fun initializeChat(user: User?) {
        // Skip if already initialized with real user data, or if still null and shown once
        if (initializedWithUser) return
        if (initializedOnce && user == null) return
        initializedOnce = true
        if (user != null) initializedWithUser = true
        messageHistory.clear()
        _messages.value = emptyList()
        viewModelScope.launch {
            val uid = repo.currentUid
            val pantryItems = uid?.let { repo.getPantry(it) } ?: emptyList()

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayLog = uid?.let { repo.getLogForDate(it, today) }

            val context = buildUserContext(user, pantryItems, todayLog?.totalCalories ?: 0f)
            messageHistory.clear()
            messageHistory.add(OpenAIClient.buildSystemMessage(context))

            val greeting = OpenAIClient.buildGreeting(user?.firstName)
            messageHistory.add(OpenAIClient.Message("assistant", greeting))
            _messages.value = listOf(ChatMessage(role = MessageRole.AI, text = greeting))
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _isLoading.value) return

        // Add user message to UI and history
        _messages.value = _messages.value + ChatMessage(role = MessageRole.USER, text = text)
        messageHistory.add(OpenAIClient.Message("user", text))

        // Add loading bubble
        val loadingId = System.currentTimeMillis() + 1
        _messages.value = _messages.value + ChatMessage(
            id = loadingId,
            role = MessageRole.AI,
            text = "",
            isLoading = true
        )
        _isLoading.value = true

        viewModelScope.launch {
            val response = OpenAIClient.chat(messageHistory.toList())
            messageHistory.add(OpenAIClient.Message("assistant", response))

            // Replace loading bubble with real response
            _messages.value = _messages.value
                .filter { it.id != loadingId } +
                ChatMessage(role = MessageRole.AI, text = response)
            _isLoading.value = false
        }
    }

    private fun buildUserContext(
        user: User?,
        pantryItems: List<PantryItem>,
        caloriesConsumed: Float
    ): String {
        if (user == null) return ""
        val pantryList = if (pantryItems.isNotEmpty())
            pantryItems.joinToString(", ") { it.name }
        else "none tracked"
        return """
            User Profile:
            - Name: ${user.firstName} ${user.lastName}
            - Age: ${user.age}, Sex: ${user.sex}
            - Weight: ${user.weight} kg, Height: ${user.height} cm
            - Blood type: ${user.bloodType.ifBlank { "not specified" }}
            - Dietary preference: ${user.dietPreference.ifBlank { "none specified" }}
            - Allergies/intolerances: ${user.allergies.ifBlank { "none" }}
            - Daily calorie goal: ${user.dailyCalorieGoal} kcal
            - Daily water goal: ${user.dailyWaterGoal} ml
            - Calories consumed today: ${caloriesConsumed.toInt()} kcal
            - Current pantry items: $pantryList

            Always personalise your responses to this user's profile and dietary restrictions.
        """.trimIndent()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ChatbotScreen(
    user: User?,
    viewModel: ChatbotViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(user) { viewModel.initializeChat(user) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgSecondary)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Color(0xFF1C3B22))
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        "AI ASSISTANT",
                        fontSize = 10.sp,
                        letterSpacing = 1.3.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF5A7A5A)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        "SPROUT",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = White,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6DBE73))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Online", fontSize = 11.sp, color = Color(0xFF5A7A5A))
                    }
                }
            }
        }

        // ── Messages ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(message = message)
            }
        }

        // ── Input Bar ─────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = BgPrimary,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Ask about recipes, nutrition…",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CardTeal,
                        unfocusedBorderColor = Divider,
                        focusedContainerColor = BgSecondary,
                        unfocusedContainerColor = BgSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CardTeal
                    ),
                    maxLines = 4
                )
                IconButton(
                    onClick = {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (inputText.isNotBlank() && !isLoading) CardTeal else Divider)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank() && !isLoading) White else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// ── Chat Bubble ───────────────────────────────────────────────────────────────

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CardTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 4.dp else 12.dp,
                        bottomEnd = if (isUser) 12.dp else 4.dp
                    )
                )
                .background(if (isUser) Brand else BgPrimary)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (message.isLoading) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    repeat(3) { i ->
                        val alpha by animateLoadingDot(delayMs = i * 200L)
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(TextSecondary.copy(alpha = alpha))
                        )
                    }
                }
            } else if (isUser) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = White,
                    lineHeight = 22.sp
                )
            } else {
                MarkdownText(text = message.text, color = TextPrimary)
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Brand),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── LaTeX → Unicode conversion ────────────────────────────────────────────────

private fun toSuperscript(s: String): String {
    val map = mapOf('0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
                    '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
                    '+' to '⁺', '-' to '⁻', 'n' to 'ⁿ', 'i' to 'ⁱ', 'a' to 'ᵃ',
                    'b' to 'ᵇ', 'c' to 'ᶜ', 'x' to 'ˣ', 'y' to 'ʸ')
    return s.map { map[it] ?: it }.joinToString("")
}

private fun toSubscript(s: String): String {
    val map = mapOf('0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
                    '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
                    '+' to '₊', '-' to '₋', 'n' to 'ₙ', 'i' to 'ᵢ', 'e' to 'ₑ',
                    'a' to 'ₐ', 'o' to 'ₒ', 'x' to 'ₓ')
    return s.map { map[it] ?: it }.joinToString("")
}

private fun latexToUnicode(latex: String): String {
    var s = latex.trim()

    // \frac{a}{b} → a/b
    s = s.replace(Regex("""\\frac\{([^}]+)\}\{([^}]+)\}""")) { "${it.groupValues[1]} / ${it.groupValues[2]}" }

    // Superscripts: ^{...} and ^x
    s = s.replace(Regex("""\^\{([^}]+)\}""")) { toSuperscript(it.groupValues[1]) }
    s = s.replace(Regex("""\^(\w)"""))         { toSuperscript(it.groupValues[1]) }

    // Subscripts: _{...} and _x
    s = s.replace(Regex("""_\{([^}]+)\}"""))  { toSubscript(it.groupValues[1]) }
    s = s.replace(Regex("""_(\w)"""))          { toSubscript(it.groupValues[1]) }

    // \sqrt{...}
    s = s.replace(Regex("""\\sqrt\{([^}]+)\}""")) { "√(${it.groupValues[1]})" }
    s = s.replace("\\sqrt", "√")

    // \text{...} → plain text
    s = s.replace(Regex("""\\text\{([^}]+)\}""")) { it.groupValues[1] }

    // Symbols
    val symbols = listOf(
        "\\times"   to "×",  "\\div"    to "÷",  "\\pm"    to "±",
        "\\approx"  to "≈",  "\\geq"    to "≥",  "\\leq"   to "≤",
        "\\neq"     to "≠",  "\\ne"     to "≠",  "\\cdot"  to "·",
        "\\alpha"   to "α",  "\\beta"   to "β",  "\\gamma" to "γ",
        "\\delta"   to "δ",  "\\pi"     to "π",  "\\sigma" to "σ",
        "\\mu"      to "μ",  "\\nu"     to "ν",  "\\rho"   to "ρ",
        "\\theta"   to "θ",  "\\lambda" to "λ",  "\\sum"   to "Σ",
        "\\prod"    to "Π",  "\\infty"  to "∞",  "\\in"    to "∈",
        "\\notin"   to "∉",  "\\subset" to "⊂",  "\\cup"   to "∪",
        "\\cap"     to "∩",  "\\forall" to "∀",  "\\exists" to "∃",
        "\\partial" to "∂",  "\\nabla"  to "∇",  "\\int"   to "∫",
        "\\left("   to "(",  "\\right)" to ")",
        "\\left["   to "[",  "\\right]" to "]",
        "\\left{"   to "{",  "\\right}" to "}",
        "\\left"    to "",   "\\right"  to "",
        "\\,"       to " ",  "\\;"      to " ",  "\\ "     to " ",
        "\\!"       to "",   "\\:"      to " ",  "{" to "", "}" to ""
    )
    symbols.forEach { (from, to) -> s = s.replace(from, to) }

    return s.trim()
}

// Splits text into alternating [plain, block-math, plain, block-math, ...] segments.
// Block math delimiters: $$...$$ and \[...\]
private fun splitBlockMath(text: String): List<Pair<String, Boolean>> {
    val segments = mutableListOf<Pair<String, Boolean>>()
    val pattern = Regex("""\$\$(.*?)\$\$|\\?\\\[(.*?)\\?\\\]""", setOf(RegexOption.DOT_MATCHES_ALL))
    var last = 0
    pattern.findAll(text).forEach { match ->
        if (match.range.first > last) segments += text.substring(last, match.range.first) to false
        val math = (match.groupValues[1].ifEmpty { match.groupValues[2] }).trim()
        segments += latexToUnicode(math) to true
        last = match.range.last + 1
    }
    if (last < text.length) segments += text.substring(last) to false
    return segments
}

// Converts $...$ and \(...\) inline math in a plain string to Unicode.
private fun convertInlineMath(text: String): String {
    var s = text
    s = s.replace(Regex("""\\\((.+?)\\\)""")) { latexToUnicode(it.groupValues[1]) }
    s = s.replace(Regex("""\$([^$\n]+?)\$""")) { latexToUnicode(it.groupValues[1]) }
    return s
}

// ── Markdown renderer ─────────────────────────────────────────────────────────

@Composable
private fun MarkdownText(text: String, color: Color, modifier: Modifier = Modifier) {
    val segments = splitBlockMath(text)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        segments.forEach { (segment, isMath) ->
            if (isMath) {
                // Block math — render in a distinct styled box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(color.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = segment,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = color,
                        lineHeight = 22.sp
                    )
                }
            } else {
                // Regular markdown — line-by-line rendering
                convertInlineMath(segment).lines().forEach { line ->
                    val trimmed = line.trimStart()
                    val isBullet   = trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")
                    val isNumbered = trimmed.length > 2 && trimmed[0].isDigit() && trimmed[1] == '.'
                    val isHeader    = trimmed.startsWith("# ")
                    val isSubHeader = trimmed.startsWith("## ")

                    when {
                        line.isBlank() -> Spacer(modifier = Modifier.height(4.dp))
                        isSubHeader -> Text(
                            text = parseInlineMarkdown(trimmed.removePrefix("## ").trim()),
                            fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = color, lineHeight = 22.sp
                        )
                        isHeader -> Text(
                            text = parseInlineMarkdown(trimmed.removePrefix("# ").trim()),
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            color = color, lineHeight = 24.sp
                        )
                        isBullet -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("•", fontSize = 14.sp, color = color, lineHeight = 22.sp)
                            Text(
                                text = parseInlineMarkdown(trimmed.drop(2).trim()),
                                fontSize = 14.sp, color = color, lineHeight = 22.sp
                            )
                        }
                        isNumbered -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val dotIdx = trimmed.indexOf('.')
                            Text(trimmed.substring(0, dotIdx + 1), fontSize = 14.sp, color = color, lineHeight = 22.sp)
                            Text(
                                text = parseInlineMarkdown(trimmed.substring(dotIdx + 1).trim()),
                                fontSize = 14.sp, color = color, lineHeight = 22.sp
                            )
                        }
                        else -> Text(
                            text = parseInlineMarkdown(line),
                            fontSize = 14.sp, color = color, lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

private fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1A000000))) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

@Composable
private fun animateLoadingDot(delayMs: Long): State<Float> {
    val transition = rememberInfiniteTransition(label = "dot")
    return transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 600,
                delayMillis = delayMs.toInt(),
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
}
