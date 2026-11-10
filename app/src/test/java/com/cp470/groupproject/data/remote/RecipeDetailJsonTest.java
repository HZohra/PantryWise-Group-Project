package com.cp470.groupproject.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.remote.dto.SubstituteRequest;
import com.cp470.groupproject.data.remote.dto.SubstituteResponse;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;

public class RecipeDetailJsonTest {

    private final Gson gson = new Gson(); // same defaults as Retrofit's GsonConverterFactory

    @Test
    public void recipe_parsesNutritionPerServing() {
        String json = "{\"recipeId\":\"edamam:x\",\"title\":\"Teriyaki Chicken\",\"calories\":2253,"
                + "\"nutrition\":{\"perServing\":true,\"calories\":376,\"protein\":27,\"carbs\":3,\"fat\":null}}";

        RecipeDto recipe = gson.fromJson(json, RecipeDto.class);

        assertEquals(2253.0, recipe.calories, 0.001);
        assertNotNull(recipe.nutrition);
        assertTrue(recipe.nutrition.perServing);
        assertEquals(Integer.valueOf(376), recipe.nutrition.calories);
        assertEquals(Integer.valueOf(27), recipe.nutrition.protein);
        assertNull(recipe.nutrition.fat);
    }

    @Test
    public void substituteResponse_parsesSourceAndItems() {
        String json = "{\"ingredient\":\"butter\",\"source\":\"local\",\"tip\":\"\","
                + "\"substitutes\":[{\"name\":\"plain yogurt\",\"amount\":\"1/2 taza\","
                + "\"notes\":\"Para hornear.\",\"inPantry\":true}]}";

        SubstituteResponse response = gson.fromJson(json, SubstituteResponse.class);

        assertEquals(SubstituteResponse.SOURCE_LOCAL, response.source);
        assertEquals(1, response.substitutes.size());
        assertEquals("plain yogurt", response.substitutes.get(0).name);
        assertTrue(response.substitutes.get(0).inPantry);
    }

    @Test
    public void substituteRequest_leavesOutNullPreferences() {
        String json = gson.toJson(new SubstituteRequest("Omelette", "egg",
                Collections.singletonList("3 eggs"), Collections.emptyList(), "es-MX", null));

        assertTrue(json, json.contains("\"ingredient\":\"egg\""));
        assertTrue(json, json.contains("\"locale\":\"es-MX\""));
        assertFalse(json, json.contains("preferences"));
    }
}