# PantryWise backend (AI assistant and recipe engine)

A small Node + Express API used by the PantryWise Android app (CP470, Group 7).
It is the only place that talks to **Claude** (AI chat, substitutions, AI recipes) and
**Edamam** (recipe search), so the API keys never ship inside the app.

Owner: Team Member 5 (AI kitchen assistant and recipe engine), branch `feature/ai-assistant-recipes`.

---

## 1. Requirements

- Node.js 20 or newer (developed on Node 24)
- An Anthropic API key (Claude)
- An Edamam Recipe Search API app id and key
- For the Android app: Android Studio with an emulator, and `adb` (comes with the Android SDK)

## 2. Setup (first time)

All commands are for **PowerShell**, starting from the project root.

```powershell
cd backend
npm install
Copy-Item .env.example .env
```

Open `backend/.env` and fill in the values:

| Variable | What it is |
|---|---|
| `PORT` | Port the API listens on. The app and the tests expect `3000`. |
| `ANTHROPIC_MODEL` | The Claude model name to use |
| `ANTHROPIC_API_KEY` | Your Anthropic API key |
| `EDAMAM_APP_ID` | Your Edamam application id |
| `EDAMAM_APP_KEY` | Your Edamam application key |

Optional (leave them out to use the defaults in `src/config/env.js`):

| Variable | What it is |
|---|---|
| `ANTHROPIC_TIMEOUT_MS` | Timeout for Claude requests, in milliseconds |
| `EDAMAM_TIMEOUT_MS` | Timeout for Edamam requests, in milliseconds |
| `API_RATE_LIMIT_PER_15_MIN` | General limit per 15 minutes (default 100) |
| `AI_RATE_LIMIT_PER_MINUTE` | AI limit per minute (default 10) |
| `RECIPES_RATE_LIMIT_PER_MINUTE` | Recipe limit per minute (default 20) |

`.env` is listed in `backend/.gitignore`. **Never commit it**, and never paste keys into
Android code, logs, screenshots or chat messages. Only `.env.example` (no real values) is committed.

## 3. Run the server

```powershell
cd backend
npm run dev
```

`npm run dev` restarts the server whenever a file changes. Use `npm start` for a normal run.

Check that it is up (in a second terminal tab):

```powershell
curl.exe -s http://localhost:3000/api/v1/health
```

Expected: a JSON body with `"status":"ok"`.

## 4. Connect the Android emulator

The app calls `http://localhost:3000`. On the emulator, "localhost" means the phone itself,
so forward the phone's port 3000 to your computer:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb wait-for-device
& $adb reverse tcp:3000 tcp:3000
& $adb reverse --list
```

Expected: the last command prints `tcp:3000 tcp:3000`.

Run this again **every time the emulator restarts** (the tunnel is lost on restart).
If the app shows a network error right after setting up the tunnel, try once more:
the first request after a fresh tunnel sometimes fails.

Open the AI screens directly (debug builds only):

```powershell
& $adb shell am start -n com.cp470.groupproject/.ui.ai.AiAssistantActivity
& $adb shell am start -n com.cp470.groupproject/.ui.ai.RecipesActivity
& $adb shell am start -n com.cp470.groupproject/.ui.ai.AiRecipeActivity
```

## 5. Scripts

| Command | What it does |
|---|---|
| `npm start` | Start the server |
| `npm run dev` | Start the server and restart on file changes |
| `npm run test:api` | Run the Postman collection with Newman (server must be running) |
| `npm run smoke:claude` | One real call to Claude, to check the key and model |
| `npm run smoke:edamam` | One real call to Edamam, to check the id and key |
| `npm run try:ranking` | Try the pantry ranking on sample data |

## 6. API

Base URL: `http://localhost:3000/api/v1`. Every request and response body is JSON.

| Method | Path | Uses | Purpose |
|---|---|---|---|
| GET | `/health` | - | Is the server running? |
| POST | `/ai/chat` | Claude | Kitchen assistant chat that knows the pantry |
| POST | `/ai/recipe` | Claude | One new recipe built around food that expires soon |
| GET | `/recipes/search` | Edamam | Search recipes by text and filters |
| POST | `/recipes/recommend` | Edamam | Recipes ranked by what the user already has (standard or Zero-Waste) |
| POST | `/recipes/substitute` | Local list, then Claude | Replacements for a missing ingredient |

### Shared pieces

**Inventory item** (used by every POST endpoint, at most 200 items):

```json
{ "name": "milk", "quantity": 1, "unit": "L", "daysUntilExpiry": 2 }
```

- `name` is required (max 100 characters). The others are optional.
- `daysUntilExpiry`: whole number. `0` = expires today, negative = already expired, `null` or missing = no expiry date.
- Expired items are never suggested as ingredients.

**Preferences** (optional object, all fields optional):

| Field | Allowed values |
|---|---|
| `diet` | `balanced`, `high-fiber`, `high-protein`, `low-carb`, `low-fat`, `low-sodium` |
| `health` | array of Edamam labels, e.g. `vegan`, `vegetarian`, `gluten-free`, `dairy-free`, `peanut-free` |
| `cuisine` | e.g. `italian`, `mexican`, `middle eastern`, `indian` |
| `mealType` | `breakfast`, `lunch`, `dinner`, `snack`, `teatime` |
| `maxTime` | minutes, 1 to 600 |
| `maxCalories` | calories **per serving**, 50 to 3000 |
| `allergies` | array of words, e.g. `["peanut", "shellfish"]` (max 20) |
| `fitnessGoal` | `lose-weight`, `build-muscle`, `maintain-weight`, `eat-healthier` (AI only) |

**Locale** (`locale`, optional): `"en"` or `"es"` (also `"es-MX"` and similar).
Other languages fall back to English. Ingredient names in AI answers are always lowercase English,
so they can be matched against the pantry; the rest of the text follows the locale.

### POST `/ai/chat`

```json
{
  "message": "What can I cook with my expiring milk?",
  "history": [
    { "role": "user", "content": "I'm vegetarian." },
    { "role": "assistant", "content": "Got it!" }
  ],
  "inventory": [ { "name": "milk", "quantity": 1, "unit": "L", "daysUntilExpiry": 2 } ],
  "preferences": { "allergies": ["peanut"] },
  "locale": "en"
}
```

- `message`: required, max 1000 characters. `history`: max 20 messages, each max 2000 characters.
- Response: `{ "reply": "..." }`

### POST `/ai/recipe`

```json
{
  "locale": "en",
  "servings": 2,
  "inventory": [
    { "name": "spinach", "quantity": 1, "unit": "bag", "daysUntilExpiry": 1 },
    { "name": "feta", "quantity": 200, "unit": "g", "daysUntilExpiry": 2 },
    { "name": "rice", "quantity": 1, "unit": "kg" }
  ],
  "preferences": { "allergies": ["peanut"] }
}
```

- `servings`: 1 to 12 (default 2).
- The inventory needs at least one item that is not expired.
- Items expiring within 3 days are the focus of the recipe.

Response:

```json
{
  "recipe": {
    "recipeId": "ai:3f0c...",
    "title": "Spinach and Feta Baked Eggs",
    "servings": 2,
    "totalTimeMinutes": 20,
    "ingredients": [
      { "name": "spinach", "amount": "1 bag", "inPantry": true, "expiring": true, "staple": false },
      { "name": "salt", "amount": "1/4 tsp", "inPantry": false, "expiring": false, "staple": true }
    ],
    "ingredientLines": ["1 bag spinach", "1/4 tsp salt"],
    "steps": ["Preheat the oven to 200 C.", "..."],
    "tip": "Serve with rice on the side."
  },
  "usedExpiring": true,
  "expiringItems": ["spinach", "feta"]
}
```

`inPantry`, `expiring` and `staple` are decided by our code, not by the AI.
If Claude's answer is unusable or contains an allergy ingredient, the server asks once more,
then answers `502 AI_BAD_FORMAT` or `502 AI_UNSAFE_RECIPE`.

### GET `/recipes/search`

```
/recipes/search?q=chicken&diet=balanced&maxTime=30&maxCalories=500&limit=3
```

| Parameter | Notes |
|---|---|
| `q` | required, max 100 characters |
| `diet`, `cuisine`, `mealType` | same values as in Preferences |
| `health` | comma-separated labels, e.g. `vegan,gluten-free` |
| `maxTime` | 1 to 600 minutes |
| `maxCalories` | 50 to 3000 per serving |
| `limit` | 1 to 20 (default 10) |

Response: `{ "count": 3, "recipes": [ ...Recipe ] }`

**Recipe** fields: `recipeId` (`edamam:<id>`), `title`, `image`, `sourceName`, `sourceUrl`,
`servings`, `totalTimeMinutes`, `calories` (whole recipe),
`nutrition` (`perServing`, `calories`, `protein`, `carbs`, `fat`), `ingredientLines`,
`ingredientNames`, `dietLabels`, `healthLabels`, `cuisineType`.

Note: Edamam `image` links are signed and **expire after about 1 hour**. Do not store them
for later (e.g. in favourites) and expect them to keep working.

### POST `/recipes/recommend`

```json
{
  "mode": "zeroWaste",
  "query": "pasta",
  "inventory": [ { "name": "spinach", "quantity": 1, "unit": "bag", "daysUntilExpiry": 1 } ],
  "preferences": { "maxCalories": 500 },
  "limit": 5
}
```

- `mode`: `standard` (default) or `zeroWaste` (only recipes that use food expiring soon).
- `query`: optional. Without it, the server searches for the most urgent pantry items.
- `limit`: 1 to 10 (default 5). An empty inventory is allowed (the server suggests common dishes).

Response:

```json
{
  "mode": "standard",
  "personalized": true,
  "broadened": false,
  "searchedFor": ["chicken breast", "spinach"],
  "count": 5,
  "recipes": [
    { "...Recipe fields": "...",
      "match": { "score": 0.67, "haveCount": 2, "totalCount": 3,
                 "have": ["chicken", "spinach"], "missing": ["garlic"], "expiringUsed": ["spinach"] } }
  ]
}
```

- Recipes are sorted best match first.
- `broadened: true` means the filters were too strict, so cuisine, meal type and time were dropped.
- `personalized: false` means there was nothing in the pantry to search for, so defaults were used.

### POST `/recipes/substitute`

```json
{
  "recipeTitle": "Spinach Omelette",
  "ingredient": "milk",
  "ingredientLines": ["3 eggs", "1/4 cup milk", "1 cup spinach"],
  "inventory": [ { "name": "plain yogurt", "quantity": 1, "unit": "cup", "daysUntilExpiry": 3 } ],
  "preferences": { "health": ["dairy-free"] },
  "locale": "en"
}
```

- `recipeTitle` (max 150) and `ingredient` (max 100) are required. `ingredientLines`: max 50 lines of 200 characters.
- Response: 1 to 3 `substitutes` (items the user already has come first, marked `inPantry: true`)
  and `source`: `"local"` (built-in list, no AI call) or `"ai"` (asked Claude).
- Diet labels and allergies are respected in both cases.

## 7. Errors

Every error has the same shape:

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "Request body is invalid", "details": ["message is required and must be a non-empty string"] } }
```

| Status | Code | Meaning |
|---|---|---|
| 400 | `VALIDATION_ERROR` | The request is wrong. `details` lists **every** problem. |
| 404 | `NOT_FOUND` | Unknown route |
| 429 | `RATE_LIMITED` | Too many requests (see below) |
| 502 | `AI_BAD_FORMAT` | Claude's answer could not be used, even after one retry |
| 502 | `AI_UNSAFE_RECIPE` | Claude kept including an allergy ingredient |

The Android app also handles `AI_RATE_LIMITED` (the AI service itself is busy) and shows
a friendly message with a Retry button for network problems and timeouts.

## 8. Rate limits and quotas

| Limiter | Applies to | Limit |
|---|---|---|
| AI | `/ai/chat`, `/ai/recipe`, `/recipes/substitute` | 10 requests per minute |
| Recipes | `/recipes/search`, `/recipes/recommend` | 20 requests per minute |
| General | every `/api` route | 100 requests per 15 minutes |

- Our Edamam plan allows **10 calls per minute**. Edamam answers are cached in memory for
  10 minutes, so repeating the same search does not use the quota (the server log shows
  `[edamam] cache hit`).
- Limits are per server, so everyone sharing one backend shares them.
- These numbers are the defaults. Each one can be changed with an optional `.env` variable (see the optional variables table in the setup section).

## 9. Testing

Start the server first (`npm run dev`), then in another tab:

```powershell
cd backend
npm run test:api
```

Expected: `requests 20 / 0 failed` and `assertions 107 / 0 failed`.

- One run sends exactly 10 requests to the AI limiter, so **wait one minute** between runs
  (and after any manual AI call).
- One run makes about 4 Claude calls and at most 5 Edamam calls.
- AI answers vary a little. If a single AI test fails once, run it again before changing code.

### Example requests

`backend/requests/` holds sample request bodies (made-up pantry data until the real
database is connected). Send one with `curl.exe` from inside `backend/`:

```powershell
curl.exe -s -X POST http://localhost:3000/api/v1/ai/chat -H "Content-Type: application/json" --data-binary "@requests/chat-valid.json"
curl.exe -s -X POST http://localhost:3000/api/v1/ai/recipe -H "Content-Type: application/json" --data-binary "@requests/ai-recipe.json"
curl.exe -s -X POST http://localhost:3000/api/v1/recipes/recommend -H "Content-Type: application/json" --data-binary "@requests/recommend-zerowaste.json"
curl.exe -s -X POST http://localhost:3000/api/v1/recipes/substitute -H "Content-Type: application/json" --data-binary "@requests/substitute-pantry.json"
```

Use `--data-binary` (not `-d`) so the file is sent exactly as written.
PowerShell may show accented letters wrongly (e.g. `Â°`); that is only a display problem.

## 10. Privacy

- The app asks for consent before anything is sent to the AI.
- Only what the AI needs is sent: the message, recent chat history, pantry item names,
  amounts and days until expiry, and the user's food preferences and allergies.
- The server does not store chats or pantry data.
- Logs never contain API keys, Edamam request URLs (they include the key) or allergy values.

## 11. Folder structure

```
backend/
  src/
    server.js        starts the server
    app.js           Express app: middleware and routes
    config/          reads .env
    routes/          URL -> middleware -> controller
    controllers/     turn a validated request into a response
    services/        Claude, Edamam, ranking, substitutions, AI recipes
    middleware/      validation, rate limits, 404 and error handling
    utils/           prompts, AppError
  postman/           Postman collection used by npm run test:api
  requests/          sample request bodies
  scripts/           smoke tests and the ranking playground
```