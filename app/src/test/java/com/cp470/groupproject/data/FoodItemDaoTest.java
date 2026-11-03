package com.cp470.groupproject.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.dao.FoodItemDao;
import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.model.ItemStatus;
import com.cp470.groupproject.data.model.StockSummary;
import com.cp470.groupproject.util.ExpiryUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Local unit test (runs on the jvm with robolectric) of the dao queries against an in-memory room database.
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class FoodItemDaoTest {
    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY = ExpiryUtils.DAY_MS;

    /** makes livedata and room run synchronously in tests */
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private FoodItemDao dao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, PantryDatabase.class)
                .allowMainThreadQueries().build();
        dao = db.foodItemDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    /** builds a test item with the given normalized name, quantity and expiry */
    private FoodItem item(String name, String normalized, double qty, long expiry) {
        FoodItem f = new FoodItem(name, qty, "pcs", 1, 1, NOW, expiry);
        f.normalizedName = normalized;
        return f;
    }

    @Test
    public void insertThenGetById() {
        long id = dao.insert(item("Milk", "milk", 1, NOW + DAY));
        FoodItem loaded = dao.getById(id);
        assertNotNull(loaded);
        assertEquals("Milk", loaded.name);
        assertEquals(ItemStatus.ACTIVE, loaded.status);
    }

    @Test
    public void updateChangesFields() {
        long id = dao.insert(item("Milk", "milk", 1, 0));
        FoodItem f = dao.getById(id);
        f.quantity = 5;
        assertEquals(1, dao.update(f));
        assertEquals(5.0, dao.getById(id).quantity, 0.0001);
    }

    @Test
    public void deleteRemovesRow() {
        long id = dao.insert(item("Milk", "milk", 1, 0));
        assertEquals(1, dao.delete(dao.getById(id)));
        assertNull(dao.getById(id));
    }

    @Test
    public void expiringSoonReturnsOnlyWindowItemsSortedAndActive() {
        dao.insert(item("Expired", "expired", 1, NOW - DAY));
        dao.insert(item("Tomorrow", "tomorrow", 1, NOW + DAY));
        dao.insert(item("In2Days", "in2days", 1, NOW + 2 * DAY));
        dao.insert(item("Later", "later", 1, NOW + 10 * DAY));
        dao.insert(item("NoDate", "nodate", 1, 0));
        long eatenId = dao.insert(item("Eaten", "eaten", 1, NOW + DAY));
        dao.updateStatus(eatenId, ItemStatus.CONSUMED);

        List<FoodItem> result = dao.getExpiringSoonSync(NOW, NOW + 3 * DAY);

        assertEquals(2, result.size());
        assertEquals("Tomorrow", result.get(0).name);
        assertEquals("In2Days", result.get(1).name);
    }

    @Test
    public void updateStatusRemovesItemFromActiveInventory() {
        long id = dao.insert(item("Milk", "milk", 1, 0));
        assertEquals(1, dao.countActive());
        dao.updateStatus(id, ItemStatus.WASTED);
        assertEquals(0, dao.countActive());
        assertEquals(ItemStatus.WASTED, dao.getById(id).status);
    }

    @Test
    public void stockSummarySumsByNormalizedNameAndIgnoresExpired() {
        dao.insert(item("2% Milk 1L", "milk", 1, NOW + DAY));
        dao.insert(item("Whole Milk", "milk", 2, 0));
        dao.insert(item("Old Milk", "milk", 4, NOW - DAY)); // expired, must not be counted
        dao.insert(item("Eggs", "eggs", 6, 0));

        List<StockSummary> stock = dao.getStockSummarySync(NOW);

        assertEquals(2, stock.size());
        assertEquals("eggs", stock.get(0).normalizedName);
        assertEquals(6.0, stock.get(0).totalQuantity, 0.0001);
        assertEquals("milk", stock.get(1).normalizedName);
        assertEquals(3.0, stock.get(1).totalQuantity, 0.0001);
    }

    @Test
    public void searchMatchesPartialNames() {
        dao.insert(item("Oat Milk", "oat milk", 1, 0));
        dao.insert(item("Eggs", "eggs", 1, 0));
        assertEquals(1, dao.searchActiveByName("milk").size());
    }

    @Test
    public void observeByLocationFiltersByStorage() {
        FoodItem fridge = item("Milk", "milk", 1, 0);
        fridge.storageLocationId = 1;
        FoodItem pantry = item("Rice", "rice", 1, 0);
        pantry.storageLocationId = 3;
        dao.insert(fridge);
        dao.insert(pantry);

        List<FoodItem> result = LiveDataTestUtil.getValue(dao.observeByLocation(3));

        assertEquals(1, result.size());
        assertEquals("Rice", result.get(0).name);
    }

    @Test
    public void freshUseSoonExpiredPartitionTheInventory() {
        long limit = NOW + 3 * DAY;
        dao.insert(item("Expired", "expired", 1, NOW - DAY));
        dao.insert(item("Soon", "soon", 1, NOW + DAY));
        dao.insert(item("Fresh", "fresh", 1, NOW + 9 * DAY));
        dao.insert(item("NoDate", "nodate", 1, 0));

        assertEquals(1, LiveDataTestUtil.getValue(dao.observeExpired(NOW)).size());
        assertEquals(1, LiveDataTestUtil.getValue(dao.observeUseSoon(NOW, limit)).size());
        assertEquals(2, LiveDataTestUtil.getValue(dao.observeFresh(limit)).size());
    }
}