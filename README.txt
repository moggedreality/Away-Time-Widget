Away Time 2.2 - a home screen widget showing how long your screen has been off today.

Requirements
- Android Studio Otter (2025.2.1) or newer, including Rabbit. It uses its bundled JDK (17 or 21).
  In Settings > Build, Execution, Deployment > Build Tools > Gradle, set "Gradle JDK" to the
  embedded/bundled JDK. Gradle 8.14 can't run on JDK 25 or newer, so don't pick one.
- Android SDK Platform 36 (Android 16). Studio offers to install it on the first sync, or use
  Tools > SDK Manager.
- Gradle 8.14.3 is fetched automatically by the included wrapper (gradlew), so the first sync needs internet.
- Versions used: Android Gradle Plugin 8.13.2, Kotlin 2.3.20, compileSdk/targetSdk 36 (Android 16),
  minSdk 28 (Android 9), Java 17 bytecode.
- Why not AGP 9.x? It only opens in the very newest Studio (Rabbit 2) and needs Gradle 9.6.
  AGP 8.13.2 supports API 36 and opens in every recent Studio, so it fails for fewer people.

Android 16 changes already handled
- Edge-to-edge is forced on: the app draws behind the bars and pads itself with window insets.
- Predictive back is on by default: back handling uses OnBackInvokedCallback.
- No orientation or resizability locks, since Android 16 ignores them on large screens.

Build the APK
1. Open this folder in Android Studio (File > Open, pick the folder that contains settings.gradle.kts).
2. Let Gradle sync, then Build > Build Bundle(s) / APK(s) > Build APK(s).
3. The file is at app/build/outputs/apk/debug/app-debug.apk. Copy it to the phone and install it.
Command line alternative: ./gradlew assembleDebug  (Windows: gradlew.bat assembleDebug).
If the phone says the app conflicts with an older Away Time, uninstall the old one first.

First run
1. Open the app. On Home, tap "Open settings" in the usage access card and switch Away Time on.
2. When Home shows "Keep the widget up to date", tap Allow and confirm. This exempts Away Time from
   Battery Saver so the refresh timer isn't paused. It's also under Settings > Battery.
3. Tap "Add to home screen" on Home (or long-press the home screen > Widgets > Away Time).

The app has four tabs
Home      Today's number, a live preview of your widget, and shortcuts.
Design    Look: theme, accent and background colors, alignment (left / center / right), card opacity,
          corner radius, padding, overall text size, auto-fit, font (or load your own .ttf/.otf).
          Text: pick a line (greeting, day, label, time, detail, bar) and set its color, size,
          letter spacing, space above and bold. The bar has its own thickness and spacing.
          Content: which lines show.
          Greetings: write your own lines. Time of day (4 slots), Hourly cycle, or Random.
          Write {name} where your name should appear.
Presets   Save the current look under a name, update, rename or delete it. Four built-in looks too.
Settings  App theme (match the widget, or pick one on its own), animations (Full / Reduced / Off),
          widget auto refresh (Off, 5, 15, 30, 60 minutes), data source, standby apps, battery exemption,
          usage access, reset.

Data source and standby apps (Settings > Data)
Counts as time on phone: "App usage" (default) counts only time with an app open, which is how Digital
Wellbeing measures it, so home screen and lock screen time doesn't count. "Screen on" counts every moment
the screen is on. Digital Wellbeing keeps its data private and has no API, so no app can read it
directly; App usage reads the same Android usage stats it is built from, so the totals come out close
but not always identical. Both modes need Usage access, and the app asks for it when you switch.
Standby apps: pick apps (a clock, music or maps app on a stand, say). Their time counts as away even with
the screen on, in both modes. Home shows how much that was. The list needs no extra permission.

Themes: Catppuccin Mocha, AMOLED black, Catppuccin Latte, Nord, Gruvbox, Rose Pine, Dracula,
Tokyo Night, Paper, plus Adaptive (a dark and a light theme of your choice that follow the device).

Animations: Full uses spring motion and staggered entrances (nothing fades). Reduced keeps short
slides only, for low-end phones. Off removes motion. Android's own "remove animations" switch is
always respected.

Battery: Android never lets an app switch Battery Saver off. What Away Time can do is ask to be put on
the "unrestricted" list, which stops Battery Saver and Doze from pausing its refresh timer. Some
phones (Xiaomi, OPPO, Vivo, Samsung) have a second auto-start / sleeping-apps list: allow it there too.

App icon: the image you supplied (res/drawable-nodpi/ic_launcher_fg.png and ic_launcher_bg.png,
used by res/mipmap-anydpi/ic_launcher.xml). To swap it later, replace those two PNGs (432x432,
keep the subject inside the middle 288 px so launcher masks don't crop it).

How it works
Android already logs screen on/off events for usage stats. The widget reads today's events,
adds up the screen-on time, and shows (time since midnight - screen-on time).
A lock screen glance counts as screen on. The widget is drawn as an image so that custom fonts,
colors and per-line styling work, which is why its text can't be selected.

If the build fails
- Read the first red line in the Build tab, not the last. Later errors are usually knock-on effects.
- "Unsupported class file major version" or "Could not determine java version": wrong Gradle JDK.
  Set it to the bundled JDK 17 or 21 (see Requirements).
- "Failed to find Platform SDK with path: platforms;android-36": install SDK Platform 36 in the SDK Manager.
- "Plugin ... was not found" or "Could not resolve": no internet, or a proxy/VPN blocking
  google() and mavenCentral(). Try File > Sync Project with Gradle Files on another connection.
- "AGP requires Android Studio ...": update Android Studio (Help > Check for Updates).
- After any change to Gradle files: Build > Clean Project, then rebuild.
