package com.cp470.groupproject.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.AiRecipeIngredientDto;
import com.cp470.groupproject.data.remote.dto.AiRecipeRequest;
import com.cp470.groupproject.data.remote.dto.AiRecipeResponse;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;

public class AiRecipeJsonTest {

    private final Gson gson = new Gson(); // same defaults as Retrofit's GsonConverterFactory

    private static final String RESPONSE_JSON = "{"
            + "\"usedExpiring\":true,\"expiringItems\":[\"spinach\",\"feta\"],"
            + "\"recipe\":{\"recipeId\":\"ai:96e344ce-460f-40f1-9a76-dd135d8370bf\","
            + "\"title\":\"Spinach and Feta Baked Eggs\",\"servings\":2,\"totalTimeMinutes\":null,"
            + "\"ingredients\":["
            + "{\"name\":\"spinach\",\"amount\":\"1 bag\",\"inPantry\":true,\"expiring\":true,\"staple\":false},"
            + "{\"name\":\"garlic\",\"amount\":\"1 clove\",\"inPantry\":false,\"expiring\":false,\"staple\":false},"
            + "{\"name\":\"salt\",\"amount\":\"1/4 tsp\",\"inPantry\":false,\"expiring\":false,\"staple\":true}],"
            + "\"ingredientLines\":[\"1 bag spinach\",\"1 clove garlic\",\"1/4 tsp salt\"],"
            + "\"steps\":[\"Preheat the oven.\",\"Wilt the spinach.\"],\"tip\":\"\"}}";

    @Test
    public void response_parsesRecipeAndFlags() {
        AiRecipeResponse response = gson.fromJson(RESPONSE_JSON, AiRecipeResponse.class);

        assertTrue(response.usedExpiring);
        assertEquals("feta", response.expiringItems.get(1));
        assertTrue(response.recipe.recipeId.startsWith("ai:"));
        assertEquals(Integer.valueOf(2), response.recipe.servings);
        assertNull(response.recipe.totalTimeMinutes);
        assertEquals(2, response.recipe.steps.size());
        assertEquals("1 bag spinach", response.recipe.ingredientLines.get(0));

        AiRecipeIngredientDto spinach = response.recipe.ingredients.get(0);
        assertTrue(spinach.inPantry);
        assertTrue(spinach.expiring);
        AiRecipeIngredientDto salt = response.recipe.ingredients.get(2);
        assertFalse(salt.inPantry);
        assertTrue(salt.staple);
    }

    @Test
    public void request_sendsLocaleAndServings_leavesOutNullPreferences() {
        String json = gson.toJson(new AiRecipeRequest(Collections.emptyList(), "es-MX", null, 2));

        assertTrue(json, json.contains("\"locale\":\"es-MX\""));
        assertTrue(json, json.contains("\"servings\":2"));
        assertFalse(json, json.contains("preferences"));
    }
}