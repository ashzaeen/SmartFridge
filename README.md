# SmartFridge

**An AI-powered nutrition and pantry assistant for Android.** Track what is in your fridge, get recipes built from what you already have, log fitness and water, and ask an AI nutrition chatbot anything in between.

For anyone who wants to cook from what is already in the fridge while keeping an eye on nutrition. Built as a four-person team project (Team 8) in Kotlin and Jetpack Compose.

> Platform: Android 8.0+ (API 26) · Recommended test device: Pixel 8 emulator, API 35

## Features

- **Pantry** - keep a live inventory of what you have; scan items with the camera (GPT-4o Vision).
- **Recipe search and Recipe Match** - find recipes with Spoonacular, ranked by what is already in your pantry.
- **Swap It** - ask for practical ingredient substitutes when you are missing something.
- **Order what is missing** - jump from a recipe to Uber Eats, DoorDash, Instacart or Amazon Fresh with the shopping list.
- **Fitness and water tracking** - daily calories, macros and water against personal goals.
- **AI nutrition chatbot** - answers using your profile (goals, allergies, diet) as context.
- **Reminders** - a daily 8:00 PM nudge via WorkManager.
- **Accounts** - sign up and log in with Firebase Authentication; data stored in Firestore.

## Tech stack

Kotlin · Jetpack Compose (Material 3) · Navigation Compose · Firebase Auth + Firestore · Retrofit / OkHttp · CameraX · WorkManager · DataStore · Coil · Spoonacular API · OpenAI API (`gpt-4o-mini` chat and substitutes, `gpt-4o` vision).

## Setup

You need Android Studio (recent stable) and your own API keys. No secrets are stored in this repo. The build uses Gradle 9.3.1 (via the wrapper), Android Gradle Plugin 8.9.1, Kotlin 2.0.21 and compile SDK 36. Gradle runs on a JDK 21 toolchain that it downloads automatically on first sync; the app code itself targets Java 11.

1. **Clone**
   ```bash
   git clone https://github.com/ashzaeen/SmartFridge.git
   cd SmartFridge
   ```
2. **Add API keys.** Copy the example file and fill it in:
   ```bash
   cp local.properties.example local.properties
   ```
   ```properties
   SPOONACULAR_API_KEY=your_spoonacular_key
   OPENAI_API_KEY=your_openai_key
   ```
   Get a Spoonacular key from the [Spoonacular Food API console](https://spoonacular.com/food-api) (free tier available) and an OpenAI key from the [OpenAI API keys page](https://platform.openai.com/api-keys). Keys are read at build time into `BuildConfig` (see `app/build.gradle.kts`). If you skip them the app still builds, but recipe search and every AI feature will fail at runtime.
3. **Add Firebase.** Create a Firebase project, register an Android app with the package name `com.team8.smartfridge`, enable **Email/Password** sign-in and **Firestore**, then download `google-services.json` into `app/`. It is git-ignored. This one is not optional: without it the Gradle build fails.
4. **Run.** Open the project in Android Studio, let Gradle sync, pick a device or emulator, and press Run. Or from the terminal:
   ```bash
   ./gradlew installDebug
   ```

> Keys that are compiled into an APK can be extracted from it. Use restricted or low-quota keys for anything you share, and never publish an APK built with a key you care about.

## Costs and data

- **Cost:** Spoonacular has a free tier (about 150 points a day). OpenAI is pay-per-use: `gpt-4o-mini` for chat and substitutes, `gpt-4o` for camera scans. Firebase's free plan covers a project this size.
- **What leaves the device:** recipe and ingredient queries go to Spoonacular. Chat messages go to OpenAI along with your profile context (goals, allergies, diet), and a photo you scan goes to OpenAI for recognition. Account sign-in and your pantry, fitness and water data live in Firebase.

## Notes and known limits

- Internet is required for login and registration, recipe search, Recipe Match, Swap It and the chatbot. Saved recipes, the pantry list, fitness and water tracking work offline.
- The Spoonacular free tier allows about 150 points per day; if results stop loading, that is the likely cause.
- The 8:00 PM reminder registers after the app has been opened once.
- Camera scanning needs a physical camera; on an emulator the simulated camera image is used.

## Project layout

```
app/src/main/java/com/team8/smartfridge/
  data/        Firebase repository
  model/       User, pantry and log entities
  network/     Spoonacular (Retrofit) and OpenAI clients
  repository/  Recipe repository
  ui/          auth, home, pantry, recipe, fitness, water, chatbot, settings, theme
  utils/       Notification worker, recipe match store
```

## Team

Team 8: Shrayosi Sarker Hridika, Mushtahir Kabir, Farhan Ishraq Khan and Ashzaeen Fatmi Khan.

This repository is a fork of [Hridika356/SmartFridge](https://github.com/Hridika356/SmartFridge).

<details>
<summary>Original course-submission details (kept for reference)</summary>

_Kept as submitted. The APK link and the test account may no longer work, and the "keys bundled in the APK" note describes that original build, not this repository._

### Team contact information

| Member | Role | Email |
| --- | --- | --- |
| Shrayosi Sarker Hridika | Developer | ssh9831@mavs.uta.edu |
| Mushtahir Kabir | Developer | mxk7416@mavs.uta.edu |
| Farhan Ishraq Khan | Developer, primary contact | fxi3751@mavs.uta.edu |
| Ashzaeen Fatmi Khan | Developer | afk6801@mavs.uta.edu |

### Installation (original grading build)

1. [Download SmartFridge.apk](https://drive.google.com/file/d/1-8ZCAQs9sJSOqfDzEVNL2eCMMBxVEh9f/view?usp=sharing)
2. Install via Android Studio Device Manager, or run `install SmartFridge.apk`.
3. Launch the SmartFridge app.

### Test credentials (pre-loaded for grading)

- Email: fishraq305@gmail.com
- Password: 654321

You can also create a new account on the registration screen; all features work the same for any user.

### Original technical notes

- All API keys (Spoonacular, OpenAI) were bundled in the APK via BuildConfig, so no setup was needed.
- Authentication (login and registration) runs on Firebase.

</details>
