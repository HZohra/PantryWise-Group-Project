package com.cp470.groupproject.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.RecipeDto;
import com.cp470.groupproject.data.remote.dto.RecommendRequest;
import com.cp470.groupproject.data.remote.dto.RecommendResponse;
import com.cp470.groupproject.data.remote.dto.SearchResponse;
import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;

public class RecipeJsonTest {

    private final Gson gson = new Gson(); // same defaults as Retrofit's GsonConverterFactory

    private static final String RECOMMEND_JSON = "{"
            + "\"mode\":\"zeroWaste\",\"personalized\":true,\"broadened\":false,"
            + "\"searchedFor\":[\"spinach\"],\"count\":1,"
            + "\"recipes\":[{"
            + "\"recipeId\":\"edamam:abc123\",\"title\":\"Spinach Omelette\","
            + "\"image\":\"https://img.example/1.jpg\",\"sourceName\":\"Food Site\","
            + "\"sourceUrl\":\"https://food.example/omelette\",\"servings\":2.0,"
            + "\"totalTimeMinutes\":null,\"calories\":420.7,"
            + "\"ingredientLines\":[\"2 eggs\",\"1 cup spinach\"],"
            + "\"ingredientNames\":[\"egg\",\"spinach\"],"
            + "\"dietLabels\":[],\"healthLabels\":[\"vegetarian\"],\"cuisineType\":[\"french\"],"
            + "\"match\":{\"score\":140,\"haveCount\":1,\"totalCount\":2,"
            + "\"have\":[\"spinach\"],\"missing\":[\"egg\"],"
            + "\"expiringUsed\":[{\"name\":\"spinach\",\"daysUntilExpiry\":1}]}"
            + "}]}";

    @Test
    public void recommendResponse_parsesEveryField() {
        RecommendResponse response = gson.fromJson(RECOMMEND_JSON, RecommendResponse.class);

        assertEquals("zeroWaste", response.mode);
        assertTrue(response.personalized);
        assertFalse(response.broadened);
        assertEquals(1, response.count);

        RecipeDto recipe = response.recipes.get(0);
        assertEquals("edamam:abc123", recipe.recipeId);
        assertEquals("Spinach Omelette", recipe.title);
        assertEquals(2.0, recipe.servings, 0.001);
        assertNull(recipe.totalTimeMinutes);
        assertEquals(2, recipe.ingredientLines.size());

        assertNotNull(recipe.match);
        assertEquals(1, recipe.match.haveCount);
        assertEquals(2, recipe.match.totalCount);
        assertEquals("egg", recipe.match.missing.get(0));
        assertEquals("spinach", recipe.match.expiringUsed.get(0).name);
        assertEquals(1, recipe.match.expiringUsed.get(0).daysUntilExpiry);
    }

    @Test
    public void searchResponse_recipesHaveNoMatch() {
        String json = "{\"count\":1,\"recipes\":[{\"recipeId\":\"edamam:x\",\"title\":\"Soup\","
                + "\"totalTimeMinutes\":45.0,\"ingredientLines\":[],\"ingredientNames\":[]}]}";

        SearchResponse response = gson.fromJson(json, SearchResponse.class);

        assertEquals(1, response.count);
        assertNull(response.recipes.get(0).match);
        assertEquals(45.0, response.recipes.get(0).totalTimeMinutes, 0.001);
    }

    @Test
    public void recommendRequest_leavesOutNullQueryAndPreferences() {
        String json = gson.toJson(new RecommendRequest(Collections.emptyList(), null, null, 5,
                RecommendRequest.MODE_ZERO_WASTE));

        assertTrue(json, json.contains("\"mode\":\"zeroWaste\""));
        assertTrue(json, json.contains("\"limit\":5"));
        assertFalse(json, json.contains("query"));
        assertFalse(json, json.contains("preferences"));
    }
}