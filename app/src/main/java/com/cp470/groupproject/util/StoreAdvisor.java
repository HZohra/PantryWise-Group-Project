package com.cp470.groupproject.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Suggests which kind of shop sells an ingredient, from its name. the criterion is the type of
 * product, not the price. plain java, so it is tested on the jvm.
 */
public final class StoreAdvisor {

    /** words of packaged or preserved food: whatever else the name says, it is a supermarket item */
    private static final Set<String> PANTRY = words(
            "sauce", "broth", "stock", "paste", "powder", "juice", "canned", "dried", "oil",
            "salsa", "caldo", "polvo", "zumo", "aceite", "lata");

    private static final Set<String> FISH = words(
            "salmon", "tuna", "cod", "shrimp", "prawn", "fish", "trout", "sardine",
            "salmón", "atún", "bacalao", "gamba", "pescado", "merluza");

    private static final Set<String> BUTCHER = words(
            "chicken", "beef", "pork", "turkey", "lamb", "bacon", "sausage", "ham", "steak", "mince",
            "pollo", "ternera", "cerdo", "pavo", "cordero", "jamón", "salchicha");

    private static final Set<String> BAKERY = words(
            "bread", "baguette", "croissant", "bun", "bagel", "tortilla", "pan", "bollo", "barra");

    private static final Set<String> PRODUCE = words(
            "tomato", "onion", "garlic", "pepper", "lettuce", "spinach", "carrot", "potato", "apple",
            "banana", "lemon", "orange", "cucumber", "mushroom", "tomate", "cebolla", "ajo",
            "pimiento", "lechuga", "zanahoria", "patata", "manzana", "plátano", "limón", "naranja");

    private StoreAdvisor() { }

    /**
     * @param name an ingredient name as typed, such as "2 red peppers"; may be null
     * @return the suggested kind of shop, never null ({@link StoreType#GENERAL} when unsure)
     */
    public static StoreType suggest(String name) {
        String normalized = IngredientNormalizer.normalize(name);
        if (normalized.isEmpty()) {
            return StoreType.GENERAL;
        }
        String[] words = normalized.split(" ");
        for (String word : words) {
            if (contains(PANTRY, word)) {
                return StoreType.GENERAL;
            }
        }
        // fish before meat and meat before produce: the first table that knows a word wins
        if (anyWordIn(words, FISH)) return StoreType.FISH;
        if (anyWordIn(words, BUTCHER)) return StoreType.BUTCHER;
        if (anyWordIn(words, BAKERY)) return StoreType.BAKERY;
        if (anyWordIn(words, PRODUCE)) return StoreType.PRODUCE;
        return StoreType.GENERAL;
    }

    /**
     * @param query what to look for, such as "fish market" or "Carnes Lopez"
     * @return a geo uri that opens a maps app searching for it, for example "geo:0,0?q=fish%20market"
     */
    public static String mapsUri(String query) {
        try {
            // URLEncoder writes a space as "+", but maps apps read "%20" better. the overload that
            // takes a Charset needs API 33, so the charset is given by name
            String encoded = URLEncoder.encode(query == null ? "" : query, "UTF-8")
                    .replace("+", "%20");
            return "geo:0,0?q=" + encoded;
        } catch (UnsupportedEncodingException e) {
            // every java platform supports UTF-8, so this cannot happen
            throw new IllegalStateException(e);
        }
    }

    private static boolean anyWordIn(String[] words, Set<String> table) {
        for (String word : words) {
            if (contains(table, word)) {
                return true;
            }
        }
        return false;
    }

    /** true if the word is in the table, also when it is a simple plural ("peppers", "tomatoes") */
    private static boolean contains(Set<String> table, String word) {
        if (table.contains(word)) {
            return true;
        }
        if (word.endsWith("es") && table.contains(word.substring(0, word.length() - 2))) {
            return true;
        }
        return word.endsWith("s") && table.contains(word.substring(0, word.length() - 1));
    }

    private static Set<String> words(String... list) {
        return new HashSet<>(Arrays.asList(list));
    }
}
