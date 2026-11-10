package com.cp470.groupproject.data.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.ChatRequest;
import com.cp470.groupproject.data.remote.dto.PreferencesDto;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;

public class ChatRequestJsonTest {

    private final Gson gson = new Gson(); // same defaults as Retrofit's GsonConverterFactory

    @Test
    public void localeIsSent_nullPreferencesAreLeftOut() {
        String json = gson.toJson(new ChatRequest("Hi", Collections.emptyList(), Collections.emptyList(),
                "es-MX", null));

        assertTrue(json, json.contains("\"locale\":\"es-MX\""));
        assertFalse(json, json.contains("preferences"));
    }

    @Test
    public void oldConstructor_sendsNoLocaleOrPreferences() {
        String json = gson.toJson(new ChatRequest("Hi", Collections.emptyList(), Collections.emptyList()));

        assertFalse(json, json.contains("locale"));
        assertFalse(json, json.contains("preferences"));
    }

    @Test
    public void preferences_useBackendFieldNames_andSkipUnsetFields() {
        PreferencesDto prefs = new PreferencesDto();
        prefs.allergies = Collections.singletonList("peanut");
        prefs.fitnessGoal = PreferencesDto.GOAL_BUILD_MUSCLE;

        String json = gson.toJson(new ChatRequest("Hi", Collections.emptyList(), Collections.emptyList(),
                "en-US", prefs));

        assertTrue(json, json.contains("\"allergies\":[\"peanut\"]"));
        assertTrue(json, json.contains("\"fitnessGoal\":\"build-muscle\""));
        assertFalse(json, json.contains("\"diet\""));
    }
}