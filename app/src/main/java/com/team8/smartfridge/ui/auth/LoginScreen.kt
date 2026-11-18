package com.team8.smartfridge.ui.auth

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import androidx.compose.material3.Text
import com.team8.smartfridge.ui.theme.PlayfairFontFamily
import com.team8.smartfridge.ui.theme.SyneFontFamily

// ── Login palette ─────────────────────────────────────────────────────────────
private val LBg        = Color(0xFF060D07)
private val LGreen     = Color(0xFF3D8C42)
private val LLite      = Color(0xFF6BBF6E)
private val LCream     = Color(0xFFEDE8DC)
private val LMuted     = LCream.copy(alpha = 0.35f)
private val LDarkFill  = Color(0xFF2D6B30)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onLoginClick: (String, String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotClick: (String) -> Unit = {},
    isLoading: Boolean = false,
    authError: String? = null,
    resetSent: Boolean = false
) {
    // ── Entry animation triggers ─────────────────────────────────────────────
    var scanLineActive   by remember { mutableStateOf(false) }
    var scanLineDone     by remember { mutableStateOf(false) }
    var bracketsVisible  by remember { mutableStateOf(false) }
    var eyebrowVisible   by remember { mutableStateOf(false) }
    var word1Visible     by remember { mutableStateOf(false) }
    var word2Visible     by remember { mutableStateOf(false) }
    var word3Visible     by remember { mutableStateOf(false) }
    var hRuleVisible     by remember { mutableStateOf(false) }
    var taglineVisible   by remember { mutableStateOf(false) }
    var formVisible      by remember { mutableStateOf(false) }
    var statusDotVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300);  scanLineActive   = true
        delay(850);  bracketsVisible  = true
        delay(50);   eyebrowVisible   = true
        delay(50);   word1Visible     = true
        delay(130);  word2Visible     = true
        delay(140);  word3Visible     = true
        delay(280);  hRuleVisible     = true
        delay(100);  taglineVisible   = true
        delay(100);  formVisible      = true
        delay(100);  statusDotVisible = true
    }

    // ── Animated floats ──────────────────────────────────────────────────────
    val scanProgress by animateFloatAsState(
        targetValue = if (scanLineActive) 1f else 0f,
        animationSpec = tween(1100, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
        finishedListener = { if (it >= 0.99f) scanLineDone = true },
        label = "scan"
    )
    val bracketsAlpha by animateFloatAsState(
        targetValue = if (bracketsVisible) 1f else 0f,
        animationSpec = tween(500), label = "brackets"
    )
    val eyebrowAlpha by animateFloatAsState(
        targetValue = if (eyebrowVisible) 1f else 0f,
        animationSpec = tween(500), label = "eyebrow"
    )
    val hRuleScale by animateFloatAsState(
        targetValue = if (hRuleVisible) 1f else 0f,
        animationSpec = tween(700, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "hRule"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (taglineVisible) 1f else 0f,
        animationSpec = tween(500), label = "tagline"
    )
    val formAlpha by animateFloatAsState(
        targetValue = if (formVisible) 1f else 0f,
        animationSpec = tween(900, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "formAlpha"
    )
    val formSlide by animateFloatAsState(
        targetValue = if (formVisible) 0f else 1f,
        animationSpec = tween(900, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
        label = "formSlide"
    )
    val statusDotAlpha by animateFloatAsState(
        targetValue = if (statusDotVisible) 1f else 0f,
        animationSpec = tween(400), label = "statusDot"
    )

    // ── Wordmark reveal heights ──────────────────────────────────────────────
    var word1H by remember { mutableFloatStateOf(0f) }
    var word2H by remember { mutableFloatStateOf(0f) }
    var word3H by remember { mutableFloatStateOf(0f) }

    val word1Slide by animateFloatAsState(
        targetValue = if (word1Visible) 0f else 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = "w1"
    )
    val word2Slide by animateFloatAsState(
        targetValue = if (word2Visible) 0f else 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = "w2"
    )
    val word3Slide by animateFloatAsState(
        targetValue = if (word3Visible) 0f else 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = "w3"
    )

    // ── Form state ───────────────────────────────────────────────────────────
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPw      by remember { mutableStateOf(false) }

    val emailSrc    = remember { MutableInteractionSource() }
    val pwSrc       = remember { MutableInteractionSource() }
    val emailFocused    by emailSrc.collectIsFocusedAsState()
    val pwFocused       by pwSrc.collectIsFocusedAsState()

    val emailIcoAlpha by animateFloatAsState(if (emailFocused) 0.7f else 0.3f, tween(300), label = "eIco")
    val pwIcoAlpha    by animateFloatAsState(if (pwFocused)    0.7f else 0.3f, tween(300), label = "pIco")
    val emailLineW    by animateFloatAsState(if (emailFocused) 1f   else 0f,   tween(350), label = "eLine")
    val pwLineW       by animateFloatAsState(if (pwFocused)    1f   else 0f,   tween(350), label = "pLine")

    // ── Button state (driven by isLoading / authError props) ────────────────
    var buttonText    by remember { mutableStateOf("SIGN IN →") }
    var buttonFilled  by remember { mutableStateOf(false) }
    var buttonClicked by remember { mutableStateOf(false) }
    var prevLoading   by remember { mutableStateOf(false) }
    val btnSrc        = remember { MutableInteractionSource() }
    val btnFillScale  by animateFloatAsState(
        targetValue = if (buttonClicked || buttonFilled) 1f else 0f,
        animationSpec = tween(350), label = "btnFill"
    )

    LaunchedEffect(buttonClicked) {
        if (buttonClicked && !buttonFilled) {
            delay(350)
            buttonClicked = false
        }
    }

    // Drive dots while loading; "Welcome back ✓" only on actual login success
    LaunchedEffect(isLoading, authError, resetSent) {
        when {
            isLoading -> {
                prevLoading = true
                val dots = listOf("·", "· ·", "· · ·")
                var i = 0
                while (true) {
                    buttonText = dots[i % 3]
                    i++
                    delay(400)
                }
            }
            prevLoading && resetSent -> {
                buttonText   = "SIGN IN →"
                buttonFilled = false
                prevLoading  = false
            }
            prevLoading && authError == null -> {
                buttonText   = "Welcome back ✓"
                buttonFilled = true
                prevLoading  = false
            }
            prevLoading && authError != null -> {
                buttonText   = "SIGN IN →"
                buttonFilled = false
                prevLoading  = false
            }
        }
    }

    // ── Screen metrics ───────────────────────────────────────────────────────
    val configuration = LocalConfiguration.current
    val density       = LocalDensity.current
    val screenHPx     = with(density) { configuration.screenHeightDp.dp.toPx() }
    val slide40Px     = with(density) { 40.dp.toPx() }

    // ── Infinite transitions ─────────────────────────────────────────────────
    val inf = rememberInfiniteTransition(label = "inf")

    val dotPulse by inf.animateFloat(
        initialValue = 0f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "dotPulse"
    )

    val ingRows      = listOf(
        "Tomatoes · Basil · Eggs · Avocado",
        "Salmon · Garlic · Broccoli · Lemon",
        "Chicken · Quinoa · Spinach · Mango",
        "Olive Oil · Ginger · Turmeric · Kale"
    )
    val ingDurations = listOf(22000, 26000, 30000, 34000)
    val ingSizes     = listOf(28.sp, 32.sp, 36.sp, 40.sp)
    val ingWidths    = remember { mutableStateListOf(2000f, 2000f, 2000f, 2000f) }

    @Suppress("NAME_SHADOWING")
    val ingScrolls = ingDurations.mapIndexed { i, dur ->
        inf.animateFloat(
            initialValue = 0f, targetValue = -1f,
            animationSpec = infiniteRepeatable(
                animation = tween(dur, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ing$i"
        )
    }

    // ── Root ─────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LBg)
    ) {

        // ── Ingredient background ────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize().zIndex(-1f)) {
            ingRows.forEachIndexed { i, rowText ->
                val scrollFraction by ingScrolls[i]
                val topPct = 0.08f + i * 0.22f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .offset(y = (configuration.screenHeightDp * topPct).dp)
                        .graphicsLayer { translationX = ingWidths[i] * scrollFraction }
                ) {
                    Text(
                        text = "$rowText   ·   $rowText   ·   $rowText",
                        style = TextStyle(
                            fontFamily = PlayfairFontFamily,
                            fontStyle  = FontStyle.Italic,
                            fontWeight = FontWeight.Black,
                            fontSize   = ingSizes[i],
                            color      = LGreen.copy(alpha = 0.06f)
                        ),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            ingWidths[i] = coords.size.width.toFloat() / 3f
                        }
                    )
                }
            }
        }

        // ── Scan line ────────────────────────────────────────────────────────
        if (!scanLineDone) {
            val scanAlpha = when {
                scanProgress < 0.05f -> scanProgress / 0.05f
                scanProgress > 0.95f -> (1f - scanProgress) / 0.05f
                else                 -> 1f
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .zIndex(10f)
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationY = screenHPx * scanProgress
                        alpha        = scanAlpha
                    }
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, LLite, LLite, Color.Transparent)
                        )
                    )
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = android.graphics.Paint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.argb(102, 107, 191, 110)
                                maskFilter = BlurMaskFilter(
                                    12.dp.toPx(), BlurMaskFilter.Blur.NORMAL
                                )
                            }
                            canvas.nativeCanvas.drawRect(
                                0f, -6.dp.toPx(), size.width, 6.dp.toPx(), paint
                            )
                        }
                    }
            )
        }

        // ── Main content column ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = configuration.screenHeightDp.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center
        ) {

            // ── Top zone ─────────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth()) {

                // Corner brackets via Canvas
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = bracketsAlpha }
                ) {
                    val sz  = 22.dp.toPx()
                    val sw  = Stroke(width = 1.5.dp.toPx())
                    val col = LLite.copy(alpha = 0.35f)
                    // top-left
                    drawPath(Path().apply {
                        moveTo(0f, sz); lineTo(0f, 0f); lineTo(sz, 0f)
                    }, col, style = sw)
                    // top-right
                    drawPath(Path().apply {
                        moveTo(size.width - sz, 0f); lineTo(size.width, 0f); lineTo(size.width, sz)
                    }, col, style = sw)
                    // bottom-left
                    drawPath(Path().apply {
                        moveTo(0f, size.height - sz); lineTo(0f, size.height); lineTo(sz, size.height)
                    }, col, style = sw)
                    // bottom-right
                    drawPath(Path().apply {
                        moveTo(size.width - sz, size.height); lineTo(size.width, size.height); lineTo(size.width, size.height - sz)
                    }, col, style = sw)
                }

                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {

                    // Eyebrow
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .graphicsLayer { alpha = eyebrowAlpha }
                    ) {
                        Box(modifier = Modifier.width(24.dp).height(1.5.dp).background(LLite))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "SMARTFRIDGE",
                            style = TextStyle(
                                fontFamily    = SyneFontFamily,
                                fontWeight    = FontWeight.Bold,
                                fontSize      = 10.sp,
                                letterSpacing = 3.5.sp,
                                color         = LLite
                            )
                        )
                    }

                    // Wordmark line 1: "Eat"
                    Box(modifier = Modifier.clipToBounds()) {
                        Text(
                            "Eat",
                            style = TextStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize   = 72.sp,
                                lineHeight = 66.sp,
                                color      = LCream
                            ),
                            modifier = Modifier
                                .onGloballyPositioned { word1H = it.size.height.toFloat() }
                                .graphicsLayer { translationY = word1H * word1Slide }
                        )
                    }

                    // Wordmark line 2: "what" — ghosted fill approximates HTML outline
                    Box(modifier = Modifier.clipToBounds()) {
                        Text(
                            "what",
                            style = TextStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Black,
                                fontStyle  = FontStyle.Italic,
                                fontSize   = 72.sp,
                                lineHeight = 66.sp,
                                color      = LCream.copy(alpha = 0.30f)
                            ),
                            modifier = Modifier
                                .onGloballyPositioned { word2H = it.size.height.toFloat() }
                                .graphicsLayer { translationY = word2H * word2Slide }
                        )
                    }

                    // Wordmark line 3: "matters."
                    Box(modifier = Modifier.clipToBounds()) {
                        Text(
                            "matters.",
                            style = TextStyle(
                                fontFamily = PlayfairFontFamily,
                                fontWeight = FontWeight.Black,
                                fontStyle  = FontStyle.Italic,
                                fontSize   = 72.sp,
                                lineHeight = 66.sp,
                                color      = LLite
                            ),
                            modifier = Modifier
                                .onGloballyPositioned { word3H = it.size.height.toFloat() }
                                .graphicsLayer { translationY = word3H * word3Slide }
                        )
                    }

                    // Horizontal rule
                    Spacer(Modifier.height(22.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .graphicsLayer {
                                scaleX           = hRuleScale
                                transformOrigin  = TransformOrigin(0f, 0.5f)
                            }
                            .background(
                                Brush.horizontalGradient(
                                    listOf(LLite, LLite.copy(alpha = 0.1f))
                                )
                            )
                    )
                    Spacer(Modifier.height(18.dp))

                    // Tagline
                    Text(
                        "Your intelligent kitchen companion",
                        style = TextStyle(
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 12.sp,
                            color      = LMuted
                        ),
                        modifier = Modifier.graphicsLayer { alpha = taglineAlpha }
                    )
                }
            }

            // ── Form section ─────────────────────────────────────────────────
            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha        = formAlpha
                        translationY = slide40Px * formSlide
                    }
            ) {
                // Form label
                Text(
                    "SIGN IN",
                    style = TextStyle(
                        fontFamily    = SyneFontFamily,
                        fontWeight    = FontWeight.Bold,
                        fontSize      = 10.sp,
                        letterSpacing = 3.sp,
                        color         = LMuted
                    )
                )
                Spacer(Modifier.height(28.dp))

                // Email field
                UnderlineField(
                    value           = email,
                    onValueChange   = { email = it },
                    placeholder     = "Email address",
                    iconAlpha       = emailIcoAlpha,
                    lineScale       = emailLineW,
                    interactionSource = emailSrc,
                    leadingIcon     = { alpha ->
                        EnvelopeIcon(tint = LCream.copy(alpha = alpha))
                    }
                )

                Spacer(Modifier.height(28.dp))

                // Password field
                UnderlineField(
                    value             = password,
                    onValueChange     = { password = it },
                    placeholder       = "Password",
                    iconAlpha         = pwIcoAlpha,
                    lineScale         = pwLineW,
                    interactionSource = pwSrc,
                    isPassword        = true,
                    showPassword      = showPw,
                    leadingIcon       = { alpha ->
                        PadlockIcon(tint = LCream.copy(alpha = alpha))
                    }
                )

                // Forgot row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = if (resetSent) 8.dp else 32.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        "Forgot?",
                        style = TextStyle(
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 11.sp,
                            color      = LLite.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (email.isNotBlank()) onForgotClick(email)
                        }
                    )
                }
                if (resetSent) {
                    Text(
                        "Reset link sent — check your email",
                        style = TextStyle(
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 12.sp,
                            color      = LLite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                }

                // Sign in button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = 1.5.dp,
                            color = LGreen,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable(
                            interactionSource = btnSrc,
                            indication        = null
                        ) {
                            if (!isLoading && !buttonFilled
                                && email.isNotBlank() && password.isNotBlank()
                            ) {
                                buttonClicked = true
                                onLoginClick(email, password)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Fill sweep
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX          = btnFillScale
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                            .background(if (buttonFilled) LDarkFill else LGreen)
                    )
                    Text(
                        buttonText,
                        style = TextStyle(
                            fontFamily    = SyneFontFamily,
                            fontWeight    = FontWeight.ExtraBold,
                            fontSize      = 13.sp,
                            letterSpacing = 3.sp,
                            color         = LCream
                        )
                    )
                }

                // Error message
                if (authError != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        authError,
                        style = TextStyle(
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 12.sp,
                            color      = Color(0xFFFF6B6B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(28.dp))

                // Sign up row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No account?",
                        style = TextStyle(
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize   = 12.sp,
                            color      = LCream.copy(alpha = 0.30f)
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Create one",
                        style = TextStyle(
                            fontFamily = SyneFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 12.sp,
                            color      = LLite
                        ),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRegisterClick() }
                    )
                }
            }
        }

        // ── Status dot (top-right) ───────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 22.dp)
                .zIndex(5f)
                .graphicsLayer { alpha = statusDotAlpha }
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(17.dp)) {
                    // Pulse ring
                    drawCircle(
                        color  = LLite.copy(alpha = (1f - dotPulse / 5f).coerceIn(0f, 0.6f)),
                        radius = (3.5.dp.toPx()) + dotPulse.dp.toPx(),
                        style  = Stroke(width = 1f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(LLite)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                "SECURE",
                style = TextStyle(
                    fontFamily    = SyneFontFamily,
                    fontWeight    = FontWeight.Bold,
                    fontSize      = 9.sp,
                    letterSpacing = 1.5.sp,
                    color         = LMuted
                )
            )
        }
    }
}

// ── Underline text field ──────────────────────────────────────────────────────

@Composable
private fun UnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    iconAlpha: Float,
    lineScale: Float,
    interactionSource: MutableInteractionSource,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    leadingIcon: @Composable (alpha: Float) -> Unit
) {
    Column {
        BasicTextField(
            value               = value,
            onValueChange       = onValueChange,
            singleLine          = true,
            interactionSource   = interactionSource,
            visualTransformation = if (isPassword && !showPassword)
                PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = TextStyle(
                fontFamily = SyneFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize   = 16.sp,
                color      = LCream
            ),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    leadingIcon(iconAlpha)
                    Spacer(Modifier.width(12.dp))
                    Box(modifier = Modifier.weight(1f).padding(vertical = 10.dp)) {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = TextStyle(
                                    fontFamily = SyneFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize   = 14.sp,
                                    color      = LCream.copy(alpha = 0.2f)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Track + animated focus line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
        ) {
            // Track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LCream.copy(alpha = 0.2f))
            )
            // Focus line with glow
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(lineScale)
                    .background(LLite)
                    .drawBehind {
                        drawIntoCanvas { canvas ->
                            val p = android.graphics.Paint().apply {
                                isAntiAlias = true
                                color = android.graphics.Color.argb(127, 107, 191, 110)
                                maskFilter = BlurMaskFilter(
                                    8.dp.toPx(), BlurMaskFilter.Blur.NORMAL
                                )
                            }
                            canvas.nativeCanvas.drawRect(
                                0f, -4.dp.toPx(), size.width, 4.dp.toPx(), p
                            )
                        }
                    }
            )
        }
    }
}

// ── SVG-equivalent icons via Canvas ──────────────────────────────────────────

@Composable
private fun EnvelopeIcon(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
        val sw = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
        // rect 14×10 at (1,3), rx=2
        drawRoundRect(
            color        = tint,
            topLeft      = Offset(1.dp.toPx(), 3.dp.toPx()),
            size         = Size(14.dp.toPx(), 10.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style        = sw
        )
        // V fold
        drawPath(
            path = Path().apply {
                moveTo(1.dp.toPx(), 5.5.dp.toPx())
                lineTo(8.dp.toPx(), 10.dp.toPx())
                lineTo(15.dp.toPx(), 5.5.dp.toPx())
            },
            color = tint,
            style = sw
        )
    }
}

@Composable
private fun PadlockIcon(tint: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
        val sw = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
        // body rect 10×7 at (3,7), rx=2
        drawRoundRect(
            color        = tint,
            topLeft      = Offset(3.dp.toPx(), 7.dp.toPx()),
            size         = Size(10.dp.toPx(), 7.dp.toPx()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
            style        = sw
        )
        // shackle arc
        drawPath(
            path = Path().apply {
                moveTo(5.dp.toPx(), 7.dp.toPx())
                lineTo(5.dp.toPx(), 5.5.dp.toPx())
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left   = 5.dp.toPx(),
                        top    = 2.dp.toPx(),
                        right  = 11.dp.toPx(),
                        bottom = 7.5.dp.toPx()
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo       = false
                )
                lineTo(11.dp.toPx(), 7.dp.toPx())
            },
            color = tint,
            style = sw
        )
        // keyhole
        drawCircle(
            color  = tint,
            radius = 1.dp.toPx(),
            center = Offset(8.dp.toPx(), 11.dp.toPx())
        )
    }
}
