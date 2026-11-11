# Testing

How PantryWise is tested: the strategy, the numbers, how to run everything, which tests prove which requirement, the
ten-minute demonstration script, and what is **not** covered.

The numbers in this document were measured on the branch `feature/integration-followup`: **341 Android unit tests**,
**71 backend tests** and **14 instrumented tests**. Re-run the commands in section 3 to refresh them.

## 1. Strategy

Logic that can be pure Java or pure JavaScript is written that way, so it is tested in milliseconds without a device.
What needs Android (the database, `LiveData`, resources) runs on the JVM with Robolectric. What needs a person (camera,
notifications, layout, translations) is checked by hand with a written script.

| Level | Tool | What it covers | Where |
|---|---|---|---|
| Pure logic | JUnit 4 | Dates, units, ingredient parsing, shopping maths, store suggestions, ranking helpers | `app/src/test/.../util`, `shopping`, `ui/mealplanner` |
| Database and repository | Robolectric, in-memory Room, `InstantTaskExecutorRule`, direct executor | DAO queries, repository writes, transactions, migrations | `app/src/test/.../data` |
| ViewModels and mappers | Robolectric and JUnit | Chat state, recipe screens, favourites, error texts, JSON mapping | `app/src/test/.../ui/ai`, `data/remote`, `scanner` |
| Dependencies replaced | Mockito (`@Mock`, `when`, `verify`, `ArgumentCaptor`) | The expiration checker and scheduler, the shopping generator, the barcode lookup | `work`, `shopping`, `scanner` |
| Backend | `node:test`, a fake `fetch` and a fake Claude | Validation, ranking, search, substitutes, AI recipe checks, barcode lookup | `backend/tests` |
| Real services | Instrumented tests, Postman collection, smoke scripts | The app and the backend against the real Claude and Edamam | `app/src/androidTest`, `backend/postman`, `backend/scripts` |
| Screens | Written manual script | Layout, navigation, permissions, notifications, rotation, English and Spanish, dark mode | [section 5](#5-manual-demonstration-script) |

**Conventions** (from the project rules):

- Unit tests mirror the main package under `app/src/test/java/com/cp470/groupproject/`.
- Names describe behaviour: `method_condition_expectedResult`. Boundaries are covered: zero, the exact limit, empty.
- Room and repository tests use Robolectric (`@RunWith(AndroidJUnit4.class)`, `@Config(sdk = 34)`) and an in-memory
  database, never the real file.
- Mocks live only in tests. Production classes take their dependencies in the constructor so a test can pass a mock.
- Backend tests need no key and no network: `helpers.js` puts placeholder settings in place and replaces `fetch` and
  Claude with fakes, and silences the logs (`SHOW_TEST_LOGS=1` shows them).

## 2. Numbers

### 2.1 Android unit tests (341, 0 failures, 0 skipped)

| Package under test | Classes | Tests | What they test |
|---|---|---|---|
| `util` | 13 | 141 | `ExpiryUtils`, `ExpirationTiming`, `IngredientNormalizer`, `IngredientParser`, `IngredientLine`, `UnitUtils`, `ShoppingCalculator`, `ShoppingInputValidator`, `ShoppingListFormatter`, `StoreAdvisor`, `StoreGrouper`, `WasteStatsCalculator`, `WeekUtils` |
| `ui.ai` | 15 | 72 | Chat, recipes, favourites, matching, missing ingredients, planning a recipe |
| `data` | 8 | 50 | `FoodItemDao`, `PantryRepository`, favourites, shopping repository, generation, `Parcelable` |
| `data.remote` | 6 | 20 | JSON of requests and replies, `ApiCalls`, `InventoryMapper` |
| `data.db` | 2 | 16 | Migrations 1 to 2 to 3 to 4 (`MigrationsTest`, `Migration3To4Test`) |
| `shopping` | 2 | 16 | `ShoppingListGenerator` (Mockito), `StorePreferences` |
| `work` | 2 | 10 | `ExpirationChecker`, `ExpirationScheduler` (both with Mockito) |
| `scanner` | 1 | 9 | `BarcodeViewModel` with a mocked backend |
| `ui.mealplanner` | 1 | 5 | `MealPlanGrouper` |
| `data.repository` | 1 | 2 | Limits of `RecipeRepository` |
| **Total** | **51** | **341** | |

Of the 51 classes, 16 use Robolectric and 5 use Mockito. The report is generated in
`app/build/reports/tests/testDebugUnitTest/index.html`.

### 2.2 Backend tests (71 real tests)

`npm test` prints **74** because three files are empty and the runner counts each empty file as one passing test (see
section 6). The real tests are:

| File | Tests | What it tests |
|---|---|---|
| `validate.test.js` | 18 | Every request checker: chat, AI recipe, search, recommend, substitute, `validateBody` |
| `recommend.test.js` | 15 | Name matching, scoring, ranking, Zero-Waste, broadening, allergens, defaults, errors |
| `product.test.js` | 10 | Barcode lookup: exact match, no match, no key, error codes, validation of the code, controller |
| `aiRecipe.test.js` | 8 | AI recipe: format check, one retry, allergen rejection |
| `substitute.test.js` | 8 | Local table first, then Claude, pantry items first |
| `edamam.test.js` | 7 | Normalizing recipes, nutrition per serving, cache, calories filter, error mapping |
| `claude-chat-prompts.test.js` | 5 | System prompt rules and limits |
| `chat.test.js`, `health.test.js`, `recommendation.test.js` | 0 | Empty files |

### 2.3 Instrumented tests (14, need an emulator and a running backend)

| Class | Tests |
|---|---|
| `AiRepositoryTest` | 3 |
| `RecipeRepositoryTest` | 3 |
| `AiApiServiceSmokeTest` | 3 |
| `AiRecipeRepositoryTest` | 2 |
| `RecipeSubstituteTest` | 2 |
| `ExampleInstrumentedTest` | 1 (the template test) |

They use the real backend, so they spend a little Claude and Edamam quota and are **not** part of `testDebugUnitTest`.

### 2.4 Other automatic checks

| Check | Command | Result when this was written |
|---|---|---|
| Lint | `.\gradlew.bat lintDebug` | 0 errors. A few "unused resource" warnings that were there before |
| Build | `.\gradlew.bat assembleDebug` | Builds |
| English and Spanish parity | A script that compares the keys of `values` and `values-es` | Every string exists in both |
| Diagrams of these documents | Rendering every Mermaid source to PNG (a syntax error stops the render) | All 34 render |

## 3. How to run

| What | Command | Notes |
|---|---|---|
| All Android unit tests | `.\gradlew.bat testDebugUnitTest` | First run downloads dependencies and needs internet |
| One class | `.\gradlew.bat testDebugUnitTest --tests "*ClassName"` | |
| Lint | `.\gradlew.bat lintDebug` | Report in `app/build/reports/lint-results-debug.html` |
| Backend unit tests | `cd backend; npm test` | No keys, no network |
| One backend file | `cd backend; node --test tests/product.test.js` | |
| Backend against the real services | `cd backend; npm run test:api` | The server must be running. Wait a minute between runs, because one run uses the whole AI limit |
| Real Claude and Edamam, one call each | `npm run smoke:claude`, `npm run smoke:edamam` | Check keys and plan |
| Instrumented tests | In Android Studio, run `app/src/androidTest` on an emulator | Backend running and `adb reverse` active |
| The manual script | [section 5](#5-manual-demonstration-script) | Needs the backend and the debug build |

Run the Android unit tests after every change to Java code, and the backend tests after every change to `backend/`.

## 4. Traceability

Each use case and story of the [specification](01-specification.md) is covered by automatic tests, by a step of the
manual script, or both. "Manual" numbers refer to the steps of the walkthrough in section 5.2.

| Requirement | Automatic tests | Manual step |
|---|---|---|
| UC-01 Browse and search (S-01 to S-03) | `FoodItemDaoTest`, `ExpiryUtilsTest` | 4, 8 |
| UC-02 Add a food item (S-04) | `PantryRepositoryTest`, `IngredientNormalizerTest` | 4 |
| UC-03 Add food by scanning (S-10 to S-13) | `BarcodeViewModelTest`, backend `product.test.js` | 4 |
| UC-04 Edit or delete (S-05) | `PantryRepositoryTest`, `FoodItemParcelTest` | 7 |
| UC-05 Record what happened (S-06) | `PantryRepositoryTest` (transaction of status and history) | 7.1 |
| UC-06 Waste summary (S-16) | `WasteStatsCalculatorTest` | 7.1 |
| UC-07 Expiration reminder (S-17 to S-19) | `ExpirationCheckerTest`, `ExpirationSchedulerTest`, `ExpirationTimingTest`, `ExpiryUtilsTest` | 2, 3, 8 |
| UC-08 Shopping list (S-21, S-23, S-24) | `ShoppingInputValidatorTest`, `ShoppingRepositoryTest`, `StoreAdvisorTest`, `StoreGrouperTest`, `StorePreferencesTest` | 6 |
| UC-09 Share the list (S-22) | `ShoppingListFormatterTest` | 6 |
| UC-10 Plan a meal (S-25 to S-27, S-29) | `MealPlanGrouperTest`, `WeekUtilsTest`, `IngredientLineTest`, `PlanMealTest` | 6 |
| UC-11 Generate the list from the plan (S-28) | `ShoppingCalculatorTest`, `ShoppingListGeneratorTest`, `ShoppingGenerationRepositoryTest`, `IngredientParserTest`, `UnitUtilsTest` | 6 |
| UC-12 Settings and help (S-07, S-08) | none | 1, 8 |
| Localization (S-09) | script that compares the string files; lint | 8 |
| Back navigation and insets (S-30, S-31) | none | 3 and every step |
| Robustness without permission or network (S-32) | `ApiCallsTest` for the network | 8 |
| Data integrity and updates | `MigrationsTest`, `Migration3To4Test`, `PantryRepositoryTest` | - |
| AI assistant and recipes (AI-UC 1 to 10) | See [AI engine, section 9](04-ai-engine.md#9-tests) | 5 |
| Backend validation, limits and errors | `validate.test.js`, `claude-chat-prompts.test.js`, Postman collection | 5 |

## 5. Manual demonstration script

This is the condensed version of the ten-minute walkthrough that the team uses to check the whole app end to end, and
that doubles as the script of the demonstration. The full version with the expected text of every step is kept by the
team.

### 5.1 Preparation (before the clock starts)

| # | Do | How |
|---|---|---|
| P1 | Start the backend | `cd backend; npm start`, then check `/api/v1/health` on the port set in `backend/.env` |
| P2 | Start an emulator with **API 33 or higher** | With Google Maps if possible |
| P3 | Connect the emulator to the backend, every time the emulator starts | `adb reverse tcp:3000 tcp:3000` (the app talks to port 3000 on the emulator; if the backend listens on another port, for example 3100, use `adb reverse tcp:3000 tcp:3100`) |
| P4 | Install the **debug** build | Run `app` in Android Studio |
| P5 | Start from a clean state | `adb shell pm clear com.cp470.groupproject` |
| P6 | Set the emulator language to **English** | It is switched to Spanish in step 8.2 |

### 5.2 Walkthrough

| Step | Do | Expected |
|---|---|---|
| 1 First start (1 min) | Open PantryWise. Allow notifications. Tap **?** | Main screen with Quick access, Discover and My storage. The permission dialog appears. The help guide opens |
| 2 Seed data (1 min) | Tap **DEV**, then button **11** (demo scenario) and button **2** (sample food). Tap button **3** (run check now) | Sample food and a meal plan are created. A notification "3 items need attention" appears |
| 3 Notification (45 s) | Tap the notification | The shopping list opens. The back arrow goes to the main screen, not out of the app |
| 4 Inventory and scanner (1 min 45 s) | Browse Fridge, Freezer and Pantry. Search `chi`. Tap **Scan Barcode**, explain the camera permission, then **Enter Barcode Manually** `049000028911` and save with quantity, unit, place and date | Items show their freshness. The search narrows. The scanner closes by itself and fills the name. "Food item saved". Rotating the screen does not reopen the scanner |
| 5 AI and recipes (2 min) | **AI Assistant**: "What can I cook with expiring milk?". **Recipes**: search `pasta`, open a recipe, add the missing ingredients twice, plan it, tap the heart, open **Favorites** | A reply from Claude. A list of recipes. The second add does not duplicate. The recipe appears in favourites |
| 6 Planner and list (1 min 45 s) | **Meal Planner**: see the demo meals. **Generate shopping list**, then **View**. Generate again. Tick, edit and delete an item with Undo. **Share** | The list has **Eggs 3, Sugar 1 tbsp, Soy sauce 1** and no flour, milk or rice. No duplicates. Grouped by shop. The share sheet opens |
| 7 Waste (30 s) | Open a food item, tap **Consumed** or **Wasted**, then **Waste Summary** | The statistics change and the period selector works |
| 8 Robustness (1 min 15 s) | Rotate while searching. Switch the emulator to **Spanish** and visit every screen. Switch to **dark mode**. Deny notifications and open the shopping list | The text and the tab survive. Everything is in Spanish. Text is readable in dark mode. A banner with **Settings** appears and the app does not crash |

Record every failed step in an incident table with its severity (blocks the demo, annoying, cosmetic).

## 6. What is not covered

| Gap | Impact | Suggested action |
|---|---|---|
| **No Espresso (UI) tests.** The dependency is configured, but the only instrumented UI test is the template | Navigation, forms and dialogs are checked only by hand. The course asks for UI tests | [Roadmap](../04-roadmap.md) item 1 |
| **Three empty backend test files** (`chat.test.js`, `health.test.js`, `recommendation.test.js`). The runner counts each as one passing test, so `npm test` reports 74 when there are 71 | The reported total overstates the coverage. The chat and health areas are covered elsewhere (prompts test, Postman collection) | Write the tests or delete the files |
| **ViewModels without tests:** `ShoppingListViewModel`, `MealPlannerViewModel`, `InventoryViewModel`, `ItemDetailViewModel`, `WasteSummaryViewModel` | Their logic is mostly delegated to tested helpers and the repository, but add, undo, generate and week navigation are only checked by hand | Add Mockito tests with a mocked repository |
| **Activities and adapters** | Layout, binding and navigation are manual | Espresso |
| **Real camera and ML Kit** | A barcode is only read by hand on a device or emulator camera. The lookup after the read is tested | Keep the manual step. A virtual scene or a photo can be used |
| **`NotificationHelper` content** (title, lines, plurals) | The text of the notification is checked by hand. The checker is tested with a mock | A Robolectric test that builds the notification |
| **Notification delivery and the 09:00 schedule on a real device** | WorkManager timing is not asserted. Only the delay calculation and the request are tested | Use the debug button for a one-minute check |
| **Permissions flows** (camera, notifications) | Manual | Espresso with `GrantPermissionRule` |
| **Code coverage is not measured.** There is no JaCoCo configuration | The course notes mention coverage, but there is no number to report | Add the JaCoCo Gradle plugin |
| **Dark mode and Spanish layout** | Only checked by eye | Screenshot tests, or a longer review |
| **Rate limits and load** | The limiter is configured and used by the Postman collection, not stress tested | Not needed for a course project |
| **Security** | Keys are not in the app, and a dependency scan is not run | Optional: `npm audit`, the Gradle dependency check |
| **Real AI quality** | Claude's answers vary. Tests use a fake Claude and check the shape and the safety rules, not the quality | Manual review of sample answers |

## 7. Before a pull request

1. `.\gradlew.bat testDebugUnitTest` with no failure.
2. `.\gradlew.bat lintDebug` with no error.
3. `cd backend; npm test` with no failure, if the backend changed.
4. Run the manual script (or the steps that touch the change) on the emulator, in English and Spanish.
5. Say in the pull request which files of other members were touched.
