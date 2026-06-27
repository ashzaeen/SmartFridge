
SmartFridge — AI-Powered Nutrition & Pantry Assistant

A native Android application bridging household inventory with personalized health tracking, 
powered by Spoonacular and OpenAI.

TEAM CONTACT INFORMATION
MEMBER CONTACT

Mushtahir Kabir
Developer
Email: mxk7416@mavs.uta.edu
Phone: (682) 323-6552

Farhan Ishraq Khan
Developer· Primary Contact
Email: fxi3751@mavs.uta.edu
Phone: (682) 241-7524

Shrayosi Sarker Hridika
Developer
Email: ssh9831@mavs.uta.edu
Phone: (210) 865-8581

Ashzaeen Fatmi Khan
Developer
Email: afk6801@mavs.uta.edu
Phone: (682) 367-8950

APPLICATION ACCESS
Platform: Android (API 26+)
Recommended: Pixel 8 emulator, API 35

INSTALLATION
1. [Locate SmartFridge.apk ](https://drive.google.com/file/d/1-8ZCAQs9sJSOqfDzEVNL2eCMMBxVEh9f/view?usp=sharing)
2. Install via Android Studio Device Manager or run: install SmartFridge.apk
3. Launch the SmartFridge app.

TEST CREDENTIALS
Pre-loaded for grading convenience:
Email: fishraq305@gmail.com
Password: 654321
You may also create a new account via the registration screen — all features work identically for 
any user.

KNOWN ISSUES & EXCEPTIONS
• Notification scheduling at 8:00 PM is configured but requires the app to be opened at 
least once for the WorkManager to register.
• The Spoonacular API has a free tier daily limit of 150 points. If results stop loading midgrading, this is the cause — wait 24 hours or use cached results.
• Camera scanning requires physical device permissions; on the emulator, the simulated 
camera image will be used instead of a live feed.

TECHNICAL NOTES 
• All API keys (Spoonacular, OpenAI) are bundled in the APK via BuildConfig and require 
no setup on your end.
• Authentication, Login and Registration is executed with Firebase. New accounts can be 
created or existing accounts can be logged in. (Test Credential Information is given).
• Internet connection is required for: Login & Registration, Recipe Search, Recipe Match, 
Swap It, AI Chatbot.
• Internet is NOT required for: Saved Recipes, Pantry list, Fitness Tracking, Water Intake.
• Tech stack: Kotlin · Jetpack Compose · Firebase · Retrofit · WorkManager · CameraX · 
Spoonacular API · OpenAI.
