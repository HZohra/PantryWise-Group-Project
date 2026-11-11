# 1. Project overview

PantryWise is an Android app, written in Java and XML, that helps a household waste less food. It keeps an inventory of
what is in the fridge, the freezer and the pantry, warns the user before food expires, turns what is about to expire
into recipe ideas, and plans the shopping so that only what is missing gets bought.

It is the group project of the Wilfrid Laurier course **CP470 - Android Programming**, built by a team of five.
This is the **follow-up (intermediate) delivery**, not the final one.

## 1.1 The problem

Food is often thrown away because nobody remembered it was in the fridge, it expired unnoticed, or a meal was never
cooked. Households also buy things they already own, because the shopping list is not connected to what is at home.
Three questions come up every week:

1. What do I have, and what is about to expire?
2. What can I cook with it?
3. What do I still need to buy?

## 1.2 The solution

PantryWise answers those three questions in one app and connects the answers: the expiring food feeds the
recipe suggestions, the planned meals feed the shopping list, and the shopping list only contains what the pantry
does not cover.

| Area | What the user can do |
|---|---|
| Pantry inventory | See food by storage place (Fridge, Freezer, Pantry), search it, add, edit and delete items, see a colour-coded freshness status |
| Expiration tracking | Get one notification every morning about food that has expired or expires within 3 days. Tapping it opens the shopping list |
| Waste statistics | Record what happened to each item (consumed, wasted, expired, donated) and see the waste rate for 7 days, 30 days or all time |
| Barcode scanning | Scan a product barcode with the camera (or type it) and let the app fill in the food name from the USDA database |
| Shopping list | Add, edit, tick, delete (with undo) and share items. The list can be grouped by the kind of shop that sells each product |
| Weekly meal planner | Plan up to four meals a day for seven days, using favourite or custom recipes, and generate the shopping list from the plan |
| AI kitchen assistant | Chat with an assistant that knows the pantry, generate a recipe from expiring food, ask for substitutes, and get step-by-step instructions |
| Recipes | Get recipe recommendations ranked by how much of the pantry they use, search recipes, use Zero-Waste Mode, save favourites |
| Settings and help | Light, dark or system theme, text size, and an in-app guide |
| Languages | The whole interface is available in English and Spanish |

Details and the implementation status of every feature are in [Features and status](02-features-status.md).

## 1.3 Users and scenarios

The target user is a person who shares a household and wants to throw away less food without keeping a spreadsheet.

**Scenario 1 - "What is going to go off?"** In the morning a notification says that three items need attention.
The user taps it, sees the shopping list, and opens the pantry to see which items are expired or should be used soon.

**Scenario 2 - "What can I cook with this?"** The user opens the AI assistant, asks what to cook with the milk that
expires tomorrow, gets an answer based on the real inventory, and saves a recipe to the favourites.

**Scenario 3 - "Plan the week and buy only what I need."** The user plans two dinners in the meal planner and taps
*Generate shopping list*. The list contains only the ingredients that are not already in the pantry, in the missing
quantities, grouped by shop.

More scenarios, written as use cases and user stories, are in the [specification](engineering/01-specification.md).

## 1.4 Team and responsibilities

| Member | Area | Branch |
|---|---|---|
| Tahmina | Main inventory screen, add and edit food forms, navigation, settings and preferences, localization, help, rotation state | `feature/inventory-ui` |
| Nora | Room database, repository, consumption history and waste statistics, Parcelable passing, database tests | `feature/database-room` |
| Isha | Barcode scanning (CameraX and ML Kit), USDA API with Retrofit, camera permission | `feature/barcode-usda-api` |
| Aitor | Expiration reminders with WorkManager, notification permission, shopping list, weekly meal planner | `feature/expiration-notifications` |
| Zohra | AI kitchen assistant, recipe engine, chatbot, favourites, backend server | `feature/ai-assistant-recipes` |

**Git flow.** Every member works on their own branch and opens pull requests into `develop`. Nothing is merged
directly into `main`. The branch `feature/integration-followup` joins the five areas into one coherent app for this
delivery (navigation, permissions, the scanner connected to the add-food form, resources and layout fixes).

## 1.5 Technology stack

| Technology | Used for | Where |
|---|---|---|
| Java 11 and XML layouts | The whole Android app (no Kotlin) | `app/` |
| Material 3 and AndroidX | UI components, `RecyclerView`, `ConstraintLayout`, light and dark themes | layouts and themes |
| ViewModel and LiveData | MVVM state that survives rotation | `ui/*/*ViewModel` |
| Room (SQLite) | Local database, version 4 | `data/` |
| WorkManager | Daily expiration check in the background | `work/` |
| Retrofit, OkHttp, Gson | HTTP calls to the backend | `data/remote/` |
| CameraX and ML Kit | Camera preview and barcode recognition | `scanner/` |
| Glide | Recipe pictures | `ui/ai/` |
| Node.js and Express | Backend that hides the API keys | `backend/` |
| Claude API (Anthropic) | AI chat, AI recipes and substitutes, called only from the backend | `backend/` |
| Edamam API | Recipe search, recommendations and nutrition, called only from the backend | `backend/` |
| USDA FoodData Central | Product name from a barcode, called only from the backend | `backend/` |
| JUnit 4, Mockito, Robolectric | Unit tests of the app | `app/src/test` |
| `node:test` | Unit tests of the backend | `backend/tests` |

## 1.6 Course concepts demonstrated

The app was designed so that each CP470 topic appears in a real feature.

| Concept | Where it appears |
|---|---|
| MVVM | Every screen: Activity (view) -> ViewModel -> repository -> DAO or API |
| Explicit intents | Main screen to every other screen. The expiration notification opens the shopping list |
| Implicit intents | Share the shopping list (`ACTION_SEND`), open a recipe in the browser and a shop in the maps app (`ACTION_VIEW`) |
| Activity lifecycle and state | Search text and selected tab survive rotation. The planner keeps its week |
| Parcelable | `FoodItem` passed from the main screen to the detail screen |
| Runtime permissions | Camera and notifications, each with an explanatory dialog and no crash when denied |
| Toast, Snackbar, AlertDialog | Feedback, undo and retry actions, confirmations (for example before the inventory is sent to the AI) |
| RecyclerView and adapters | Inventory, shopping list, planner, recipes, favourites, chat |
| Layouts, styles and themes | `GridLayout` form with custom styles in the shopping list, `ConstraintLayout` elsewhere |
| SharedPreferences | Theme, text size, Zero-Waste Mode, shop names |
| Localization | English default and Spanish for every string |
| Room | Local persistence with migrations |
| Logging | `Log.d/i/w/e` with a `TAG` per class |
| Testing | JUnit, Mockito and Robolectric. See [Testing](engineering/05-testing.md) |
| Documentation | Javadoc on public classes and UML diagrams. See the [design](engineering/02-design.md) and [AI engine](engineering/04-ai-engine.md) documents |
