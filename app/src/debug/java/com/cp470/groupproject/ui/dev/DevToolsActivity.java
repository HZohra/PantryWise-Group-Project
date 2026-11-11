package com.cp470.groupproject.ui.dev;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkManager;

import com.cp470.groupproject.activities.MainActivity;
import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.entity.FavoriteRecipe;
import com.cp470.groupproject.data.entity.MealPlan;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.entity.StorageLocation;
import com.cp470.groupproject.data.model.MealType;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.notification.NotificationPermissionHelper;
import com.cp470.groupproject.ui.mealplanner.MealPlannerActivity;
import com.cp470.groupproject.ui.shoppinglist.ShoppingListActivity;
import com.cp470.groupproject.util.ExpiryUtils;
import com.cp470.groupproject.util.WeekUtils;
import com.cp470.groupproject.util.WindowInsetsHelper;
import com.cp470.groupproject.work.ExpirationScheduler;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Developer tools, only compiled into debug builds (source set {@code app/src/debug}).
 * lets us fill the database with sample data and trigger the expiration check while the real inventory screen
 * does not exist yet. the buttons are built in code, so it needs no layout or strings.
 */
public class DevToolsActivity extends AppCompatActivity {

    /** stored in {@link FoodItem#barcode} to recognise (and later delete) the sample items */
    private static final String SAMPLE_MARKER = "DEV-SEED";

    /** same idea for the demo stock, so buttons 2 and 11 can be used together without erasing each other */
    private static final String DEMO_MARKER = "DEV-DEMO";

    private NotificationPermissionHelper permissionHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // must be created in onCreate, before the activity is started
        permissionHelper = new NotificationPermissionHelper(this);

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(32, 32, 32, 32);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        setContentView(scroll);
        WindowInsetsHelper.applySystemBarPadding(scroll);

        TextView status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, 24);
        list.addView(status);
        // live view of the daily work registered by PantryWiseApp (ENQUEUED, RUNNING, ...)
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(ExpirationScheduler.UNIQUE_WORK_NAME)
                .observe(this, infos -> status.setText(infos.isEmpty()
                        ? "Daily check: NOT SCHEDULED"
                        : "Daily check: " + infos.get(0).getState()));

        // works even when this screen was the first one opened (adb), where "back" would leave the app
        addButton(list, "<- Back to main screen", () -> {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });
        addButton(list, "1. Request notification permission", permissionHelper::ensurePermission);
        addButton(list, "2. Fill database with sample food (replaces previous sample)", this::fillDatabaseWithSampleFood);
        addButton(list, "3. Run expiration check NOW",
                () -> ExpirationScheduler.from(this).runNow());
        addButton(list, "4. Open Shopping list",
                () -> startActivity(ShoppingListActivity.newIntent(this)));
        addButton(list, "5. Clear sample food", this::clearSampleFood);
        addButton(list, "6. Schedule TEST periodic (first run in ~1 min)", () -> {
            ExpirationScheduler.from(this).schedulePeriodic(
                    ExpirationScheduler.DEBUG_WORK_NAME, 60_000, 15, TimeUnit.MINUTES,
                    ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE);
            Toast.makeText(this, "Test scheduled", Toast.LENGTH_SHORT).show();
        });
        addButton(list, "7. Cancel TEST periodic", () -> {
            ExpirationScheduler.from(this).cancel(ExpirationScheduler.DEBUG_WORK_NAME);
            Toast.makeText(this, "Test cancelled", Toast.LENGTH_SHORT).show();
        });
        addButton(list, "8. Fill shopping list with 4 sample items (adds each time)",
                this::fillShoppingListWithSamples);
        addButton(list, "9. Save 2 sample favorite recipes", this::saveSampleFavorites);
        addButton(list, "10. Open Meal planner",
                () -> startActivity(MealPlannerActivity.newIntent(this)));
        addButton(list, "11. Fill DEMO scenario (stock + meal plan)", this::fillDemoScenario);
    }

    /**
     * stock: whole milk 1 L, flour 300 g, eggs 1, rice 1 kg. plan: pancakes for dinner today and
     * rice bowl for lunch tomorrow. "generate" must then propose Eggs 3, Sugar 1 tbsp, Soy sauce 1.
     * it replaces the previous sample food, the meals of the current week and the auto-generated
     * shopping items.
     */
    private void fillDemoScenario() {
        PantryRepository repo = new PantryRepository(getApplication());
        ZoneId zone = ZoneId.systemDefault();
        PantryDatabase.WRITE_EXECUTOR.execute(() -> {
            removeSample(repo, DEMO_MARKER);
            long now = System.currentTimeMillis();
            long weekStart = WeekUtils.startOfWeek(now, zone, WeekUtils.defaultFirstDayOfWeek());
            for (MealPlan plan : repo.getMealPlansSync(weekStart,
                    WeekUtils.endOfWeek(weekStart, zone))) {
                repo.deleteMealPlan(plan);
            }
            repo.replaceAutoGeneratedShoppingItems(new ArrayList<>(), null);

            long expires = now + 30 * ExpiryUtils.DAY_MS;
            // category ids: 1 dairy, 4 pantry
            insert(repo, DEMO_MARKER, "Whole Milk 1L", 1, "L", 1, StorageLocation.FRIDGE_ID, expires);
            insert(repo, DEMO_MARKER, "Flour", 300, "g", 4, StorageLocation.PANTRY_ID, expires);
            insert(repo, DEMO_MARKER, "Eggs", 1, "pcs", 1, StorageLocation.FRIDGE_ID, expires);
            insert(repo, DEMO_MARKER, "Rice", 1, "kg", 4, StorageLocation.PANTRY_ID, expires);

            long today = WeekUtils.startOfDay(now, zone);
            repo.addMealPlan(new MealPlan(today, MealType.DINNER, "dev-pancakes", "Pancakes",
                    Arrays.asList("200 g flour", "2 eggs", "300 ml milk", "1 tbsp sugar")), null);
            repo.addMealPlan(new MealPlan(WeekUtils.plusDays(today, 1, zone), MealType.LUNCH,
                    "dev-rice-bowl", "Rice bowl",
                    Arrays.asList("150 g rice", "2 eggs", "soy sauce")), null);
        });
        Toast.makeText(this, "Demo scenario filled", Toast.LENGTH_SHORT).show();
    }

    /** saves two recipes the meal planner dialog can pick from (saving twice just replaces them) */
    private void saveSampleFavorites() {
        PantryRepository repo = new PantryRepository(getApplication());
        repo.saveFavoriteRecipe(new FavoriteRecipe("dev-pancakes", "Pancakes",
                Arrays.asList("200 g flour", "2 eggs", "300 ml milk", "1 tbsp sugar")));
        repo.saveFavoriteRecipe(new FavoriteRecipe("dev-rice-bowl", "Rice bowl",
                Arrays.asList("150 g rice", "2 eggs", "soy sauce")));
        Toast.makeText(this, "Favorite recipes saved", Toast.LENGTH_SHORT).show();
    }

    /** adds four ready-made items so the shopping list can be seen without typing */
    private void fillShoppingListWithSamples() {
        PantryRepository repo = new PantryRepository(getApplication());
        repo.addShoppingItem(new ShoppingItem("Eggs", 12, "pcs", "Walmart"), null);
        repo.addShoppingItem(new ShoppingItem("Flour", 1, "kg", null), null);
        repo.addShoppingItem(new ShoppingItem("Bananas", 6, "", null), null);
        repo.addShoppingItem(new ShoppingItem("Milk", 2, "L", "Costco"), null);
        Toast.makeText(this, "Shopping list filled", Toast.LENGTH_SHORT).show();
    }

    private void addButton(LinearLayout parent, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    /**
     * inserts five items with different freshness: one expired, two expiring soon, and two that
     * must not appear in the alert (fresh, and no date)
     */
    private void fillDatabaseWithSampleFood() {
        PantryRepository repo = new PantryRepository(getApplication());
        // reads of the repository ending in Sync must not run on the main thread
        PantryDatabase.WRITE_EXECUTOR.execute(() -> {
            removeSample(repo, SAMPLE_MARKER);
            long now = System.currentTimeMillis();
            long day = ExpiryUtils.DAY_MS;
            // category ids follow Category.DEFAULT_KEYS: 1 dairy, 3 meat, 4 pantry
            insert(repo, SAMPLE_MARKER, "Milk 1L", 1, "L", 1, StorageLocation.FRIDGE_ID, now - 2 * day);
            insert(repo, SAMPLE_MARKER, "Yogurt", 4, "pcs", 1, StorageLocation.FRIDGE_ID, now + day);
            insert(repo, SAMPLE_MARKER, "Chicken", 500, "g", 3, StorageLocation.FRIDGE_ID, now + 2 * day);
            insert(repo, SAMPLE_MARKER, "Rice", 1, "kg", 4, StorageLocation.PANTRY_ID, now + 90 * day);
            insert(repo, SAMPLE_MARKER, "Tea", 1, "pcs", 4, StorageLocation.PANTRY_ID, 0);
        });
        Toast.makeText(this, "Database filled with sample food", Toast.LENGTH_SHORT).show();
    }

    private void clearSampleFood() {
        PantryRepository repo = new PantryRepository(getApplication());
        PantryDatabase.WRITE_EXECUTOR.execute(() -> removeSample(repo, SAMPLE_MARKER));
        Toast.makeText(this, "Sample food cleared", Toast.LENGTH_SHORT).show();
    }

    private void insert(PantryRepository repo, String marker, String name, double quantity, String unit,
                        long categoryId, long locationId, long expirationDate) {
        FoodItem item = new FoodItem(name, quantity, unit, categoryId, locationId,
                System.currentTimeMillis(), expirationDate);
        item.barcode = marker;
        repo.insertFoodItem(item, null);
    }

    /** deletes the active items created with the given marker. background thread only */
    private void removeSample(PantryRepository repo, String marker) {
        for (FoodItem item : repo.getActiveItemsSync()) {
            if (marker.equals(item.barcode)) {
                repo.deleteFoodItem(item, null);
            }
        }
    }
}
