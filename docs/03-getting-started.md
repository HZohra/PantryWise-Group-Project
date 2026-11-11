# 3. Getting started

This guide takes you from a fresh clone to a running app with the AI features working. The commands are for
**Windows PowerShell** (the team's platform). On macOS or Linux use `./gradlew` instead of `gradlew.bat`.

## 3.1 Requirements

| Requirement | Notes |
|---|---|
| Android Studio | A version that supports Android Gradle Plugin **9.3.1** (see `gradle/libs.versions.toml`) |
| JDK | Use the one bundled with Android Studio (the `jbr` folder). The code itself targets Java 11 |
| Android SDK Platform 37 | Installed from *SDK Manager* in Android Studio |
| Emulator | An Android Virtual Device with **API 33 or higher** (the notification permission dialog exists from API 33). `minSdk` is 30 |
| `adb` | Comes with the Android SDK (`%LOCALAPPDATA%\Android\Sdk\platform-tools`) |
| Node.js | **20 or newer** (developed on 24). Needed only for the backend |
| API keys | Anthropic (Claude), Edamam (Recipe Search) and USDA FoodData Central. Details in 3.3 |

The inventory, the shopping list, the meal planner, the waste statistics and the expiration reminders work without
any key and without the backend. The keys and the backend are needed for the AI assistant, the recipes and the
barcode lookup.

## 3.2 Get the code

```powershell
git clone <repository-url>
cd CP470-Group-Project
git checkout develop
```

`develop` is the integration branch. Feature work happens in `feature/*` branches and is merged into `develop` through
pull requests. Nothing is merged directly into `main`.

## 3.3 Configuration and secrets

All API keys live in **one place**, `backend/.env`, which is ignored by git. **Never commit it** and never paste a
key into the Android code, a log, a screenshot or a chat. The Android app contains no key. `local.properties` only
holds the path of the Android SDK and is ignored as well.

### Backend keys

```powershell
cd backend
npm install
Copy-Item .env.example .env
```

Open `backend/.env` and fill it in:

| Variable | Meaning | Default |
|---|---|---|
| `PORT` | Port the server listens on | `3000` |
| `ANTHROPIC_API_KEY` | Anthropic API key (required) | - |
| `ANTHROPIC_MODEL` | Claude model | `claude-haiku-5-5` |
| `EDAMAM_APP_ID` and `EDAMAM_APP_KEY` | Edamam Recipe Search credentials (required) | - |
| `USDA_API_KEY` | USDA FoodData Central key for the barcode lookup. Optional: without it only the scanner lookup fails. Free key at <https://fdc.nal.usda.gov/api-key-signup>. `DEMO_KEY` works but is rate limited | - |
| `ANTHROPIC_TIMEOUT_MS`, `EDAMAM_TIMEOUT_MS`, `USDA_TIMEOUT_MS` | Request timeouts in milliseconds | 20000, 10000 and 10000 |
| `API_RATE_LIMIT_PER_15_MIN`, `AI_RATE_LIMIT_PER_MINUTE`, `RECIPES_RATE_LIMIT_PER_MINUTE` | Rate limits | 100, 10 and 20 |

The server refuses to start if a required variable is missing (the USDA key is the only optional key). Edamam changes its plans from time to time, so check
that your account includes the Recipe Search API.

## 3.4 Start the backend

```powershell
cd backend
npm start          # or: npm run dev   (restarts when a file changes)
```

Expected output: `PantryWise backend running at http://localhost:3000`. Check it from a second terminal:

```powershell
curl.exe -s http://localhost:3000/api/v1/health
```

The answer must contain `"status":"ok"`. Leave the server running while you use the app.

### If the port is not available on Windows

Windows can reserve ranges of ports (Hyper-V, WSL or Docker do it), including port 3000. The server then fails with
`EACCES: permission denied`. List the reserved ranges:

```powershell
netsh interface ipv4 show excludedportrange protocol=tcp
```

If 3000 is inside a range, pick a free port such as `3100`: set `PORT=3100` in `backend/.env`, and use that port in the
`adb reverse` command of the next section. The Android code does not change.

## 3.5 Connect the emulator to the backend

On the emulator, `localhost` is the emulator itself, not your computer. The debug build calls
`http://localhost:3000/api/v1/`, and `adb reverse` forwards that port to the backend on your computer:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000
```

If the backend uses another port (for example 3100), forward to it: `reverse tcp:3000 tcp:3100`.

Repeat this command **every time the emulator starts or is cold booted**. Check it with:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse --list
```

Debug builds are allowed to use plain `http` to `localhost` and `10.0.2.2` (see
`app/src/debug/res/xml/network_security_config.xml`). Release builds only accept HTTPS.

## 3.6 Build and run the app

**Android Studio:** open the project folder, wait for the Gradle sync, select the `app` configuration and the
emulator, and press *Run*. The app opens on the main screen, **PantryWise**.

**Command line:**

```powershell
.\gradlew.bat assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/`.

On the first launch the app first shows the **food profile** screen (name and an answer about allergies are required),
and when it closes the system asks to allow notifications. Camera permission is requested the first time the
scanner opens.

## 3.7 Developer tools (debug builds only)

Debug builds show a small **DEV** button at the top right of the main screen, next to help and settings. It opens
`DevToolsActivity`, which is compiled only into debug builds (`app/src/debug`) and does not exist in release builds.

| Button | What it does |
|---|---|
| 2 | Fills the pantry with sample food: one item already expired, two that expire within 3 days, and two that must not trigger an alert |
| 3 | Runs the expiration check now, so the notification appears immediately |
| 6 / 7 | Schedules or cancels a test check that runs about one minute later |
| 9 | Saves two sample favourite recipes (Pancakes and Rice bowl) |
| 11 | Loads the demo scenario: pantry stock plus a meal plan, so *Generate shopping list* proposes eggs, sugar and soy sauce |
| **Back to main screen** | Returns to the home screen |

To start again from a clean state (no data, no permissions):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell pm clear com.cp470.groupproject
```

The demo scenario and the sample food use separate markers, so buttons 2 and 11 can be used together. Items that were
marked as consumed or wasted do not appear in alerts.

## 3.8 Tests, lint and documentation

| Task | Command | Expected result |
|---|---|---|
| Android unit tests | `.\gradlew.bat testDebugUnitTest` | 382 tests, 0 failures. Report in `app/build/reports/tests/testDebugUnitTest/index.html` |
| One test class | `.\gradlew.bat testDebugUnitTest --tests "*ClassName"` | |
| Lint | `.\gradlew.bat lintDebug` | 0 errors. Report in `app/build/reports/lint-results-debug.html` |
| Javadoc | `.\gradlew.bat generateJavadoc` | HTML in `app/doc/javadoc/` (not committed) |
| Backend tests | `cd backend; npm test` | 80 tests, 0 failures, no keys or network needed (the runner prints 83 because three test files are empty) |
| Backend smoke tests | `npm run smoke:claude`, `npm run smoke:edamam` | Call the real services with your keys |
| Backend API collection | `npm run test:api` | Runs the Postman collection against a running server |

The first Gradle run downloads the dependencies and needs internet. The instrumented tests in `app/src/androidTest`
(AI repositories) need an emulator and a running backend and are not part of `testDebugUnitTest`.

## 3.9 Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| The AI assistant says *"Can't reach the assistant"* | The backend is not running, or `adb reverse` was lost | Start the backend, check `/api/v1/health`, run `adb reverse` again (3.4 and 3.5) |
| `npm start` prints "running" and exits at once (older versions of `server.js`) or fails with `EACCES` | The port is reserved by Windows | Use another port (3.4). The current `server.js` prints the real error |
| Barcode lookup says *"Unable to retrieve product information"* | The backend answered with an error: usually there is no `USDA_API_KEY` in `backend/.env`, or USDA refused it | Add the key (3.3) and restart the backend |
| Barcode lookup says *"Network error"* | The backend is not running or `adb reverse` was lost | Same as the assistant row above |
| Barcode lookup says the product was not found | The product is not in the USDA branded foods database | Type the food name by hand. A test code that exists is `049000028911` |
| *Missing required environment variables* when starting the backend | `backend/.env` is missing or incomplete | Copy `.env.example` and fill in the keys |
| Recipes fail with an authentication error | Edamam credentials are wrong or the plan does not include the Recipe Search API | Check the account and the two variables |
| *Run expiration check* shows no notification | No active item has expired or expires within 3 days, or notifications are denied | Press button 2 in the developer tools again, then 3. Check the notification permission |
| The camera is black in the emulator | The virtual camera is not configured | In the AVD settings use *VirtualScene* or *Webcam*, or enter the barcode manually |
| Gradle sync fails on the plugin version | Android Studio is older than AGP 9.3.1 | Update Android Studio |
| Tests fail on the first run | Dependencies are still being downloaded, or there is no internet | Run again with internet |
| The app shows old data after changing the database code | The emulator keeps the previous database | Run `pm clear` (3.7) or uninstall the app |
