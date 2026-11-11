package com.cp470.groupproject.data.preferences;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/** The "Hide recipes that contain my allergies" switch. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class HideAllergensTest {

    private Context context;
    private AiPreferencesStore store;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        store = AiPreferencesStore.from(context);
        store.clear();
    }

    @Test
    public void offByDefault() {
        assertFalse(store.loadHideAllergens());
        assertFalse(AiPreferencesStore.currentHideAllergens(context));
        assertFalse(AiPreferencesStore.currentHideAllergens(null));
    }

    @Test
    public void savedValue_isReadByTheNextRequest() {
        store.saveHideAllergens(true);

        assertTrue(AiPreferencesStore.currentHideAllergens(context));
    }

    @Test
    public void clear_turnsItOffAgain() {
        store.saveHideAllergens(true);

        store.clear();

        assertFalse(store.loadHideAllergens());
    }
}