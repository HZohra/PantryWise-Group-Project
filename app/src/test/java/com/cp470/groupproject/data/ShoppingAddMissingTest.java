package com.cp470.groupproject.data;

import static org.junit.Assert.assertEquals;

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

import java.util.Arrays;

/** addShoppingItemsIfMissing: never duplicates a pending item. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ShoppingAddMissingTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private PantryRepository repository;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, PantryDatabase.class).allowMainThreadQueries().build();
        repository = new PantryRepository(db, Runnable::run);
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void addsNewItems_andReportsHowMany() {
        final int[] added = new int[1];
        repository.addShoppingItemsIfMissing(Arrays.asList(
                new ShoppingItem("Garlic", 2, "pcs", null),
                new ShoppingItem("Olive oil", 1, "pcs", null)), r -> added[0] = r);

        assertEquals(2, added[0]);
        assertEquals(2, repository.getShoppingItemsSync().size());
    }

    @Test
    public void itemAlreadyPending_isNotAddedAgain() {
        repository.addShoppingItem(new ShoppingItem("Garlic", 1, "pcs", null), null);
        final int[] added = new int[1];

        repository.addShoppingItemsIfMissing(Arrays.asList(
                new ShoppingItem("garlic", 2, "pcs", null),
                new ShoppingItem("Rice", 1, "pcs", null)), r -> added[0] = r);

        assertEquals(1, added[0]);
        assertEquals(2, repository.getShoppingItemsSync().size());
    }

    @Test
    public void tickedItem_canBeAddedAgain() {
        final long[] id = new long[1];
        repository.addShoppingItem(new ShoppingItem("Garlic", 1, "pcs", null), r -> id[0] = r);
        repository.setShoppingItemChecked(id[0], true);
        final int[] added = new int[1];

        repository.addShoppingItemsIfMissing(Arrays.asList(new ShoppingItem("Garlic", 1, "pcs", null)),
                r -> added[0] = r);

        assertEquals(1, added[0]);
    }

    @Test
    public void repeatsInsideTheSameRequest_areAddedOnce() {
        final int[] added = new int[1];
        repository.addShoppingItemsIfMissing(Arrays.asList(
                new ShoppingItem("Egg", 1, "pcs", null),
                new ShoppingItem("egg", 2, "pcs", null)), r -> added[0] = r);

        assertEquals(1, added[0]);
    }
}