package com.cp470.groupproject.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Tests which kind of shop is suggested for an ingredient and the maps uri. */
public class StoreAdvisorTest {

    @Test
    public void suggest_chicken_isButcher() {
        assertEquals(StoreType.BUTCHER, StoreAdvisor.suggest("chicken"));
    }

    @Test
    public void suggest_chickenBreast_isButcher() {
        assertEquals(StoreType.BUTCHER, StoreAdvisor.suggest("chicken breast"));
    }

    @Test
    public void suggest_chickenBroth_isGeneral() {
        assertEquals(StoreType.GENERAL, StoreAdvisor.suggest("chicken broth"));
    }

    @Test
    public void suggest_tomatoSauce_isGeneral() {
        assertEquals(StoreType.GENERAL, StoreAdvisor.suggest("tomato sauce"));
    }

    @Test
    public void suggest_redPeppersPlural_isProduce() {
        assertEquals(StoreType.PRODUCE, StoreAdvisor.suggest("red peppers"));
    }

    @Test
    public void suggest_tomatoes_isProduce() {
        assertEquals(StoreType.PRODUCE, StoreAdvisor.suggest("tomatoes"));
    }

    @Test
    public void suggest_salmon_isFish() {
        assertEquals(StoreType.FISH, StoreAdvisor.suggest("salmon"));
    }

    @Test
    public void suggest_cannedTuna_isGeneral() {
        assertEquals(StoreType.GENERAL, StoreAdvisor.suggest("canned tuna"));
    }

    @Test
    public void suggest_bread_isBakery() {
        assertEquals(StoreType.BAKERY, StoreAdvisor.suggest("bread"));
    }

    @Test
    public void suggest_spanishPollo_isButcher() {
        assertEquals(StoreType.BUTCHER, StoreAdvisor.suggest("pollo"));
    }

    @Test
    public void suggest_spanishPimientos_isProduce() {
        assertEquals(StoreType.PRODUCE, StoreAdvisor.suggest("Pimientos rojos"));
    }

    @Test
    public void suggest_rice_isGeneral() {
        assertEquals(StoreType.GENERAL, StoreAdvisor.suggest("rice"));
    }

    @Test
    public void suggest_upperCase_matchesIgnoringCase() {
        assertEquals(StoreType.BUTCHER, StoreAdvisor.suggest("BEEF"));
    }

    @Test
    public void suggest_nullOrBlank_isGeneral() {
        assertEquals(StoreType.GENERAL, StoreAdvisor.suggest(null));
        assertEquals(StoreType.GENERAL, StoreAdvisor.suggest("   "));
    }

    @Test
    public void suggest_withQuantityText_ignoresNumbers() {
        assertEquals(StoreType.BUTCHER, StoreAdvisor.suggest("500 g chicken"));
    }

    @Test
    public void suggest_fishAndMeat_fishWins() {
        assertEquals(StoreType.FISH, StoreAdvisor.suggest("fish and chicken"));
    }

    @Test
    public void mapsUri_spaces_areEncodedAsPercent20() {
        assertEquals("geo:0,0?q=fish%20market", StoreAdvisor.mapsUri("fish market"));
    }

    @Test
    public void mapsUri_accents_areUtf8Encoded() {
        assertEquals("geo:0,0?q=carnicer%C3%ADa", StoreAdvisor.mapsUri("carnicería"));
    }

    @Test
    public void mapsUri_null_hasEmptyQuery() {
        assertEquals("geo:0,0?q=", StoreAdvisor.mapsUri(null));
    }
}
