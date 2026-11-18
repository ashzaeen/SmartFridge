package com.team8.smartfridge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.team8.smartfridge.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Inter — headings, display text, screen titles (crisp geometric sans-serif)
val InterFont = GoogleFont("Inter")

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

// Playfair Display — editorial wordmark (weight 900, italic)
val PlayfairDisplayFont = GoogleFont("Playfair Display")

val PlayfairFontFamily = FontFamily(
    Font(googleFont = PlayfairDisplayFont, fontProvider = provider, weight = FontWeight.Black),
    Font(googleFont = PlayfairDisplayFont, fontProvider = provider, weight = FontWeight.Black, style = FontStyle.Italic),
)

// Syne — login UI labels, buttons, eyebrow text
val SyneFont = GoogleFont("Syne")

val SyneFontFamily = FontFamily(
    Font(googleFont = SyneFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = SyneFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = SyneFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = SyneFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

// IBM Plex Mono — data values, labels, tags, metadata, terminal readouts
val IbmPlexMonoFont = GoogleFont("IBM Plex Mono")

val IbmPlexMonoFamily = FontFamily(
    Font(googleFont = IbmPlexMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = IbmPlexMonoFont, fontProvider = provider, weight = FontWeight.SemiBold),
)

val Typography = Typography(
    // Display — large headings (Syne ExtraBold)
    displayLarge  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    displaySmall  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = (-0.2).sp),
    // Headlines (Syne SemiBold)
    headlineLarge  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    headlineMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    headlineSmall  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    // Titles (Syne)
    titleLarge  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 15.sp),
    titleSmall  = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal,   fontSize = 14.sp),
    // Body (Syne for prose, IBM Plex Mono for data)
    bodyLarge  = TextStyle(fontFamily = InterFontFamily,    fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = IbmPlexMonoFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    bodySmall  = TextStyle(fontFamily = IbmPlexMonoFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp),
    // Labels (IBM Plex Mono — terminal/data readout style)
    labelLarge  = TextStyle(fontFamily = IbmPlexMonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = IbmPlexMonoFamily, fontWeight = FontWeight.Normal,   fontSize = 11.sp),
    labelSmall  = TextStyle(fontFamily = IbmPlexMonoFamily, fontWeight = FontWeight.Normal,   fontSize = 10.sp, letterSpacing = 0.8.sp),
)
