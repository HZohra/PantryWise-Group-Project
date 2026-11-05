package com.cp470.groupproject.util;

/**
 * Kinds of shop the shopping list can suggest. the declaration order is the order of the groups on
 * screen, with the general supermarket last. plain java, so it is tested on the jvm.
 */
public enum StoreType {
    BUTCHER("butcher"),
    FISH("fish"),
    PRODUCE("produce"),
    BAKERY("bakery"),
    GENERAL("general");

    private final String key;

    StoreType(String key) {
        this.key = key;
    }

    /**
     * @return a fixed text that identifies the type in the saved preferences. it is not name(), so
     *         renaming a constant never loses what the user already saved
     */
    public String getKey() {
        return key;
    }
}
