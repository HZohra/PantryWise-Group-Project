package com.cp470.groupproject.data.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.PreferencesDto;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Cleaning rules (same as the backend's validate.js) and the DTO sent with requests. */
public class AiPreferencesTest {

    @Test
    public void cleanAllergy_lowercaseSingleSpacesAndAnyAlphabet() {
        assertEquals("tree nuts", AiPreferences.cleanAllergy("  Tree   Nuts "));
        assertEquals("maní", AiPreferences.cleanAllergy("Maní"));
        assertEquals("o'brien sauce", AiPreferences.cleanAllergy("O'Brien sauce"));
        assertEquals("gluten-free", AiPreferences.cleanAllergy("gluten-free"));
    }

    @Test
    public void cleanAllergy_rejectsWhatTheBackendWouldReject() {
        assertNull(AiPreferences.cleanAllergy("peanut2"));
        assertNull(AiPreferences.cleanAllergy("<script>"));
        assertNull(AiPreferences.cleanAllergy("-peanut"));
        assertNull(AiPreferences.cleanAllergy("   "));
        assertNull(AiPreferences.cleanAllergy(null));
        assertNull(AiPreferences.cleanAllergy(repeat("a", AiPreferences.MAX_ALLERGY_LENGTH + 1)));
    }

    @Test
    public void constructor_dropsInvalidAndDuplicateAllergies_andCapsTheCount() {
        List<String> many = new ArrayList<>(Arrays.asList("Peanut", "peanut", "bad1"));
        for (int i = 0; i < 30; i++) many.add("food " + letter(i));

        AiPreferences prefs = new AiPreferences(many, null, null, null);

        assertEquals("peanut", prefs.allergies.get(0));
        assertFalse(prefs.allergies.contains("bad1"));
        assertEquals(AiPreferences.MAX_ALLERGIES, prefs.allergies.size());
    }

    @Test
    public void constructor_keepsOnlyKnownLabelsDietsAndGoals() {
        AiPreferences prefs = new AiPreferences(null,
                Arrays.asList("Vegan", "martian", "vegan", "gluten-free"), "LOW-CARB", "fly");

        assertEquals(Arrays.asList("vegan", "gluten-free"), prefs.health);
        assertEquals("low-carb", prefs.diet);
        assertNull(prefs.fitnessGoal);
    }

    @Test
    public void healthLabels_followTheScreenOrder() {
        AiPreferences prefs = new AiPreferences(null, Arrays.asList("kosher", "vegetarian"), null, null);

        assertEquals(Arrays.asList("vegetarian", "kosher"), prefs.health);
    }

    @Test
    public void empty_sendsNoPreferencesAtAll() {
        assertTrue(AiPreferences.empty().isEmpty());
        assertNull(AiPreferences.empty().toDto());
        assertNull(new AiPreferences(Collections.singletonList("123"), null, "keto", null).toDto());
    }

    @Test
    public void toDto_fillsOnlyWhatIsSet() {
        AiPreferences prefs = new AiPreferences(Collections.singletonList("peanut"), null, null,
                PreferencesDto.GOAL_BUILD_MUSCLE);

        PreferencesDto dto = prefs.toDto();

        assertEquals(Collections.singletonList("peanut"), dto.allergies);
        assertNull(dto.health);
        assertNull(dto.diet);
        assertEquals("build-muscle", dto.fitnessGoal);
        assertNull(dto.cuisine);
        assertNull(dto.maxTime);
    }

    @Test
    public void withAllergy_andWithoutAllergy() {
        AiPreferences prefs = AiPreferences.empty().withAllergy(" Shellfish ").withAllergy("peanut");

        assertEquals(Arrays.asList("shellfish", "peanut"), prefs.allergies);
        assertEquals(prefs.allergies, prefs.withAllergy("bad!").allergies);
        assertEquals(Collections.singletonList("peanut"), prefs.withoutAllergy("shellfish").allergies);
    }

    @Test
    public void lists_cannotBeChangedFromOutside() {
        AiPreferences prefs = AiPreferences.empty().withAllergy("peanut");
        try {
            prefs.allergies.add("milk");
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("allergies should be read-only");
    }

    private static String repeat(String text, int times) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < times; i++) out.append(text);
        return out.toString();
    }

    /** "a", "b", … "z", "aa", "ab"… (allergies may only contain letters) */
    private static String letter(int i) {
        return i < 26 ? String.valueOf((char) ('a' + i)) : "a" + (char) ('a' + i - 26);
    }
}