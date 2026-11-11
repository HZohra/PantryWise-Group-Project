package com.cp470.groupproject.data.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.remote.dto.PreferencesDto;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

/** What every AI request carries: the saved profile, read at request time. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class CurrentPreferencesTest {

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AiPreferencesStore.from(context).clear();
    }

    @Test
    public void nothingSaved_requestsCarryNoPreferences() {
        assertNull(AiPreferencesStore.currentDto(context));
        assertNull(AiPreferencesStore.currentDto(null));
    }

    @Test
    public void savedProfile_isSentWithoutTheName() {
        AiPreferencesStore.from(context).saveProfile("Alex",
                new AiPreferences(Arrays.asList("peanut", "shellfish"), Collections.singletonList("vegan"),
                        "low-carb", "build-muscle"), false);

        PreferencesDto dto = AiPreferencesStore.currentDto(context);

        assertEquals(Arrays.asList("peanut", "shellfish"), dto.allergies);
        assertEquals(Collections.singletonList("vegan"), dto.health);
        assertEquals("low-carb", dto.diet);
        assertEquals("build-muscle", dto.fitnessGoal);
    }

    @Test
    public void noAllergiesAndNothingElse_sendsNoPreferences() {
        AiPreferencesStore.from(context).saveProfile("Alex", AiPreferences.empty(), true);

        assertNull(AiPreferencesStore.currentDto(context));
    }

    @Test
    public void anEdit_appliesToTheNextRequest() {
        AiPreferencesStore store = AiPreferencesStore.from(context);
        store.saveProfile("Alex", new AiPreferences(Collections.singletonList("peanut"), null, null, null), false);
        store.saveProfile("Alex", new AiPreferences(Collections.singletonList("milk"), null, null, null), false);

        assertEquals(Collections.singletonList("milk"), AiPreferencesStore.currentDto(context).allergies);
    }
}