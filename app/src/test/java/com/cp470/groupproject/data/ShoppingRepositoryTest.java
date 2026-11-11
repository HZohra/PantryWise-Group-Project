package com.cp470.groupproject.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.entity.ShoppingItem;
import com.cp470.groupproject.data.repository.PantryRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.List;

/** Tests the shopping list methods of the repository with an in-memory database. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ShoppingRepositoryTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private PantryRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, PantryDatabase.class)
                .allowMainThreadQueries().build();
        repository = new PantryRepository(db, Runnable::run);
    }

    @After
    public void tearDown() {
        db.close();
    }

    private List<ShoppingItem> currentList() {
        return LiveDataTestUtil.getValue(repository.getShoppingItems());
    }

    @Test
    public void deleteShoppingItem_removesRow() {
        repository.addShoppingItem(new ShoppingItem("Eggs", 12, "pcs", null), null);
        ShoppingItem saved = currentList().get(0);

        repository.deleteShoppingItem(saved);

        assertTrue(currentList().isEmpty());
    }

    @Test
    public void updateShoppingItem_renormalizesName() {
        repository.addShoppingItem(new ShoppingItem("Eggs", 12, "pcs", null), null);
        ShoppingItem saved = currentList().get(0);

        saved.name = "2% Milk 1L";
        repository.updateShoppingItem(saved);

        assertEquals("milk", currentList().get(0).normalizedName);
    }
}
