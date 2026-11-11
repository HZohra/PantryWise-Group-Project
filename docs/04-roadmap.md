# 4. Roadmap

What comes after this follow-up delivery. Priority is **High** (needed for a complete final delivery), **Medium**
(clear value, can wait) or **Low** (nice to have). Effort is a rough estimate: **S** under a day, **M** one to three
days, **L** more than three days. The suggested owner follows the current areas of the team.

## 4.1 Before the final delivery

| # | Item | Why | Priority | Effort | Suggested owner |
|---|---|---|---|---|---|
| 1 | Add Espresso tests for the main flows: the main screen opens each destination, the scanner fills the add-food form | The course asks for UI tests and the project has none | High | M | Isha (scanner), Tahmina (main screen) |
| 2 | Run the manual test script on a clean emulator in English and Spanish, in light and dark mode, and fix the incidents | Catches layout and translation problems that unit tests cannot see | High | M | Team |
| 3 | Merge `feature/integration-followup` into `develop` through a pull request and notify the owners of the files that were touched | Keeps the five branches consistent | High | S | Aitor |
| 4 | Record the demonstration (see the ten-minute walkthrough in [Testing](engineering/05-testing.md)) | Final delivery asks for a demonstration | High | S | Team |
| 5 | Agree on who runs the backend during the demonstration. For now it runs locally on one computer | Without it the AI and recipe screens do not work | High | S | Team |

## 4.2 Functional improvements

| # | Item | Why | Priority | Effort | Suggested owner |
|---|---|---|---|---|---|
| 6 | **Recipe filter panel** (diet, cuisine, calories) that starts from the saved food profile | Completes use case "Search and filter recipes" | Medium | M | Zohra |
| 7 | Use Edamam nutrition values in chat answers about a recipe | Completes the user story "Ask about nutrition". Nutrition is already shown on the recipe screen | Medium | M | Zohra |
| 8 | Scanner: show a Snackbar with a **Retry** action on a network error instead of a toast | The assignment asks for it, and it matches the rest of the app | Medium | S | Isha |
| 9 | **Saved frequent items** for faster food entry | Part of the scanner assignment, not started | Medium | M | Isha |
| 10 | Banner on the main screen with the items that need attention today | Brings the expiration alert inside the app | Low | S | Tahmina |
| 11 | Let the user choose the time of the daily reminder and the warning window (now fixed at 09:00 and 3 days) | Gives control over the notification | Low | M | Aitor |
| 12 | Mark leftovers explicitly so Zero-Waste Mode can use them | The mode uses expiring items only | Low | M | Zohra |

## 4.3 Technical debt

| # | Item | Why | Priority | Effort |
|---|---|---|---|---|
| 13 | Rate limit per device instead of per IP address | One device behind a shared network can block the others | Medium | S |
| 14 | Remove the remaining fixed colours and sizes in the add-food form and finish a review of the dark theme on every screen | Only the main, add-food and detail screens use colour resources with a night variant | Medium | M |
| 15 | Split `PantryRepository` (about 500 lines) by area, keeping a single entry point for the screens | It mixes inventory, history, shopping list, meal plan and favourites | Low | M |
| 16 | Add a continuous integration job that runs `testDebugUnitTest`, `lintDebug` and `npm test` on every pull request | Prevents a broken `develop` | Medium | M |
| 17 | Make the instrumented AI tests run against a fake backend | They need a running server and an emulator today | Low | M |
| 18 | Accessibility review with TalkBack (content descriptions, focus order, text scaling) | Touch targets are 48 dp, but the screens were not tested with a screen reader | Low | M |

## 4.4 Longer term

| # | Item | Notes |
|---|---|---|
| 19 | Host the backend (any small Node hosting service) and put the real HTTPS address in the release build. The release build currently points to `example.invalid`, so the AI and recipe screens only work in debug builds | The local backend is the official way to run the project for now |
| 20 | Accounts and cloud sync, so a household shares one pantry across phones | Requires a server-side database |
| 21 | More languages beyond English and Spanish | All strings are already in resource files |
| 22 | Home screen widget with the items that expire today | Reuses the expiration checker |
| 23 | Tablet and landscape layouts | The layouts are designed for portrait phones |

## 4.5 Order of work

1. Items 1 to 5 first: they make the delivery complete and demonstrable. The food profile and allergy safety, which were the biggest gap, are already done.
2. The rest can be taken in any order according to the time left.
