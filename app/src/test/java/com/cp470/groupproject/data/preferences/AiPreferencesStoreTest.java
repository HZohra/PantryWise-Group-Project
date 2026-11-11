package com.cp470.groupproject.data.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;

/** Saving and loading the food preferences on the phone. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class AiPreferencesStoreTest {

    private SharedPreferences file;
    private AiPreferencesStore store;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        file = context.getSharedPreferences(AiPreferencesStore.FILE_NAME, Context.MODE_PRIVATE);
        file.edit().clear().commit();
        store = new AiPreferencesStore(file);
    }

    @Test
    public void nothingSaved_loadsEmpty() {
        assertTrue(store.load().isEmpty());
    }

    @Test
    public void saveThenLoad_givesTheSamePreferences() {
        AiPreferences saved = new AiPreferences(Arrays.asList("tree nuts", "maní"),
                Arrays.asList("vegan", "gluten-free"), "high-protein", "build-muscle");

        store.save(saved);
        AiPreferences loaded = store.load();

        assertEquals(Arrays.asList("tree nuts", "maní"), loaded.allergies);
        assertEquals(Arrays.asList("vegan", "gluten-free"), loaded.health);
        assertEquals("high-protein", loaded.diet);
        assertEquals("build-muscle", loaded.fitnessGoal);
    }

    @Test
    public void invalidValuesInTheFile_areCleanedOnLoad() {
        file.edit()
                .putString(AiPreferencesStore.KEY_ALLERGIES, "peanut\nbad1\n")
                .putString(AiPreferencesStore.KEY_DIET, "keto")
                .commit();

        AiPreferences loaded = store.load();

        assertEquals(Arrays.asList("peanut"), loaded.allergies);
        assertNull(loaded.diet);
    }

    @Test
    public void clear_removesOnlyTheFoodPreferences() {
        file.edit().putBoolean("zero_waste_mode", true).commit(); // RecipeViewModel's key, same file
        store.save(new AiPreferences(Arrays.asList("peanut"), null, "low-fat", null));

        store.clear();

        assertTrue(store.load().isEmpty());
        assertTrue(file.getBoolean("zero_waste_mode", false));
    }
}