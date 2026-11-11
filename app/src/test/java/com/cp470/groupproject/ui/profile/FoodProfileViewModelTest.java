package com.cp470.groupproject.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.preferences.AiPreferences;
import com.cp470.groupproject.data.preferences.AiPreferencesStore;
import com.cp470.groupproject.data.preferences.FoodProfileRules;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

/** Editing and saving the food profile. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class FoodProfileViewModelTest {

    private Application app;
    private AiPreferencesStore store;

    @Before
    public void setUp() {
        app = ApplicationProvider.getApplicationContext();
        SharedPreferences file = app.getSharedPreferences(AiPreferencesStore.FILE_NAME, Context.MODE_PRIVATE);
        file.edit().clear().commit();
        store = new AiPreferencesStore(file);
    }

    private FoodProfileViewModel open() {
        FoodProfileViewModel viewModel = new FoodProfileViewModel(app, store);
        viewModel.loadOnce();
        return viewModel;
    }

    @Test
    public void firstTime_nothingSaved_setupNotDone() {
        FoodProfileViewModel viewModel = open();

        assertEquals("", viewModel.getInitialName());
        assertTrue(viewModel.getAllergies().isEmpty());
        assertFalse(store.isSetupDone());
    }

    @Test
    public void addAllergy_cleansRejectsAndLimits() {
        FoodProfileViewModel viewModel = open();

        assertEquals(FoodProfileViewModel.AddResult.ADDED, viewModel.addAllergy("  Tree  Nuts "));
        assertEquals(FoodProfileViewModel.AddResult.ADDED, viewModel.addAllergy("tree nuts")); // duplicate
        assertEquals(FoodProfileViewModel.AddResult.INVALID, viewModel.addAllergy("peanut2"));
        assertEquals(Collections.singletonList("tree nuts"), viewModel.getAllergies());

        for (int i = 0; i < AiPreferences.MAX_ALLERGIES; i++) viewModel.addAllergy("food " + (char) ('a' + i));
        assertEquals(FoodProfileViewModel.AddResult.LIMIT, viewModel.addAllergy("one more"));
    }

    @Test
    public void noAllergies_onlyWhileTheListIsEmpty() {
        FoodProfileViewModel viewModel = open();
        viewModel.addAllergy("peanut");

        viewModel.setNoAllergies(true);

        assertFalse(viewModel.isNoAllergies());
    }

    @Test
    public void save_withProblems_savesNothing() {
        FoodProfileViewModel viewModel = open();

        assertEquals(FoodProfileRules.Problem.NAME_MISSING, viewModel.save(""));
        assertEquals(FoodProfileRules.Problem.ALLERGIES_UNANSWERED, viewModel.save("Alex"));
        assertFalse(store.isSetupDone());
    }

    @Test
    public void save_storesEverythingAndMarksSetupDone() {
        FoodProfileViewModel viewModel = open();
        viewModel.addAllergy("peanut");
        viewModel.setHealth("vegan", true);
        viewModel.setDiet("high-protein");
        viewModel.setFitnessGoal("build-muscle");

        assertEquals(FoodProfileRules.Problem.NONE, viewModel.save("  Alex "));

        assertTrue(store.isSetupDone());
        assertEquals("Alex", store.loadName());
        AiPreferences saved = store.load();
        assertEquals(Collections.singletonList("peanut"), saved.allergies);
        assertEquals(Collections.singletonList("vegan"), saved.health);
        assertEquals("high-protein", saved.diet);
        assertEquals("build-muscle", saved.fitnessGoal);
        assertFalse(store.loadNoAllergies());
    }

    @Test
    public void save_withNoAllergiesTicked_isRemembered() {
        FoodProfileViewModel viewModel = open();
        viewModel.setNoAllergies(true);

        assertEquals(FoodProfileRules.Problem.NONE, viewModel.save("Alex"));

        assertTrue(store.loadNoAllergies());
        assertTrue(open().isNoAllergies());
    }

    @Test
    public void reopening_loadsTheSavedProfile() {
        store.saveProfile("Alex", new AiPreferences(Arrays.asList("peanut", "shellfish"),
                Collections.singletonList("gluten-free"), "low-carb", null), false);

        FoodProfileViewModel viewModel = open();

        assertEquals("Alex", viewModel.getInitialName());
        assertEquals(Arrays.asList("peanut", "shellfish"), viewModel.getAllergies());
        assertTrue(viewModel.hasHealth("gluten-free"));
        assertEquals("low-carb", viewModel.getDiet());
    }
}