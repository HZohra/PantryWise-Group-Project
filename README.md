# PantryWise

PantryWise is an Android app (Java and XML) that helps a household waste less food. It keeps an inventory of the
fridge, the freezer and the pantry, warns the user before food expires, suggests recipes for what is about to go
off, and builds a shopping list with only what is missing.

It is the group project of the Wilfrid Laurier course **CP470 - Android Programming** (Group 7). This version is
the **follow-up (intermediate) delivery**, not the final one.

## Features

| Area | What the user can do |
|---|---|
| Pantry inventory | Browse food by storage place, search it, add, edit and delete items, see a colour-coded freshness status |
| Expiration reminders | One notification every morning (about 09:00) for food that has expired or expires within 3 days. Tapping it opens the shopping list |
| Waste statistics | Record what happened to each item (consumed, wasted, expired, donated) and see the waste rate over 7 days, 30 days or all time |
| Barcode scanning | Scan a barcode with the camera, or type it, and fill in the food name from the USDA database |
| Shopping list | Add, edit, tick, delete (with undo) and share items, grouped by the kind of shop |
| Weekly meal planner | Plan up to four meals a day for a week and generate the shopping list from the plan |
| AI kitchen assistant | Chat with an assistant that knows the pantry, generate a recipe from expiring food, ask for substitutes |
| Recipes | Recommendations ranked by how much of the pantry they use, search, Zero-Waste Mode, favourites |
| Settings and help | Light, dark or system theme, text size, in-app guide |
| Languages | English and Spanish |

The implementation status of every feature (implemented, partial or planned) is in
[docs/02-features-status.md](docs/02-features-status.md).

## Architecture

```
Activity / XML layout  ->  ViewModel  ->  Repository  ->  Room (SQLite)  /  Retrofit  ->  Node backend  ->  Claude, Edamam, USDA
```

- **Android app** in `app/`: Java 11, MVVM with `ViewModel` and `LiveData`, Room 2.6.1, WorkManager, CameraX with
  ML Kit, Retrofit, Material 3. `minSdk` 30, `targetSdk` and `compileSdk` 37.
- **Backend** in `backend/`: Node.js and Express. It sits between the app and the external services so that the API
  keys never ship inside the APK. The AI assistant uses Anthropic Claude; recipe search uses Edamam; barcode lookup
  uses USDA FoodData Central.

The inventory, the shopping list, the meal planner, the waste statistics and the reminders work **without the
backend and without any key**. The backend is needed for the AI assistant, the recipes and the barcode lookup.

## Quick start

Requirements: Android Studio (with Android Gradle Plugin 9.3.1 support), Android SDK Platform 37, an emulator with
**API 33 or higher**, and Node.js 20 or newer for the backend.

```powershell
git clone https://github.com/HZohra/CP470-Group-Project.git
cd CP470-Group-Project
git checkout develop
```

**Run the app.** Open the project in Android Studio, let Gradle sync, and run the `app` configuration on an emulator
(or `.\gradlew.bat assembleDebug` to build the APK). On macOS or Linux use `./gradlew`.

**Run the backend** (only for the AI, recipe and barcode features):

```powershell
cd backend
npm install
copy .env.example .env      # then fill in the keys
npm start
adb reverse tcp:3000 tcp:3000
```

Put your Anthropic, Edamam and USDA keys in `backend/.env`. That file is git-ignored and is the only place where
secrets live. If port 3000 is not available on your machine, change `PORT` in `.env` and map it with
`adb reverse tcp:3000 tcp:<your port>`. The full guide, including troubleshooting, is in
[docs/03-getting-started.md](docs/03-getting-started.md).

## Tests

| Task | Command |
|---|---|
| Android unit tests (341) | `.\gradlew.bat testDebugUnitTest` |
| Lint | `.\gradlew.bat lintDebug` |
| Backend tests (71) | `cd backend; npm test` |
| Javadoc | `.\gradlew.bat generateJavadoc` (output in `app/doc/javadoc/`) |

Unit tests use JUnit 4, Robolectric and Mockito. See [docs/engineering/05-testing.md](docs/engineering/05-testing.md)
for the strategy, the traceability table, the manual demonstration script and what is not covered.

## Documentation

Everything is in [`docs/`](docs/README.md):

| Document | Content |
|---|---|
| [Project overview](docs/01-project-overview.md) | The problem, the solution, scenarios, team and stack |
| [Features and status](docs/02-features-status.md) | What is implemented, partial or planned |
| [Getting started](docs/03-getting-started.md) | Setup, configuration, running, troubleshooting |
| [Roadmap](docs/04-roadmap.md) | What comes next |
| [Specification](docs/engineering/01-specification.md) | Use cases, user stories, state and activity diagrams |
| [Design](docs/engineering/02-design.md) | Classes, class and object diagrams, data model |
| [Architecture](docs/engineering/03-architecture.md) | Layers, navigation, data flow, deployment |
| [AI engine](docs/engineering/04-ai-engine.md) | Chatbot workflow, recipe ranking, backend contract |
| [Testing](docs/engineering/05-testing.md) | Test strategy, numbers, manual script |
| [Backend README](backend/README.md) | Endpoints, configuration, tests of the server |

## Use of generative AI

Generative AI tools, like Claude, are used to help develop this project, including writing and reviewing code, tests and
documentation. The team reviews, runs and tests what these tools produce, and remains responsible for the final
result. Separately, the app itself calls Anthropic Claude through the backend for the AI kitchen assistant.

## Team

| Member | Area | Branch |
|---|---|---|
| Tahmina | Main inventory screen, forms, navigation, settings, localization | `feature/inventory-ui` |
| Nora | Room database, repository, waste statistics | `feature/database-room` |
| Isha | Barcode scanning and USDA lookup | `feature/barcode-usda-api` |
| Aitor | Expiration reminders, shopping list, meal planner | `feature/expiration-notifications` |
| Zohra | AI assistant, recipe engine, backend server | `feature/ai-assistant-recipes` |

Work happens in one branch per feature and is merged into `develop` through pull requests. Nothing is merged
directly into `main`. The branch `feature/integration-followup` joins the five areas for this delivery.
