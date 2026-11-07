package com.cp470.groupproject.shopping;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.cp470.groupproject.util.StoreType;

import java.util.EnumMap;
import java.util.Map;

/**
 * Small settings of the shopping list kept in a SharedPreferences file: the name the user gives
 * to each kind of shop and whether the list is grouped by shop. it is the only class that touches
 * that file.
 */
public class StorePreferences {

    /** name of the preferences file, private to this app and not shared with other settings */
    static final String FILE_NAME = "pantrywise_stores";
    static final String KEY_GROUP_BY_STORE = "group_by_store";
    private static final String KEY_STORE_PREFIX = "store_name_";

    private final SharedPreferences prefs;

    /** @param prefs the preferences to use; a mock in unit tests */
    public StorePreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /** @return the preferences stored in the app's private file */
    public static StorePreferences from(@NonNull Context context) {
        return new StorePreferences(
                context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE));
    }

    /** @return the names the user saved, only for the kinds of shop that have one */
    @NonNull
    public Map<StoreType, String> getStoreNames() {
        Map<StoreType, String> names = new EnumMap<>(StoreType.class);
        for (StoreType type : StoreType.values()) {
            String name = prefs.getString(KEY_STORE_PREFIX + type.getKey(), null);
            if (name != null) {
                names.put(type, name);
            }
        }
        return names;
    }

    /**
     * saves the name of a kind of shop. a blank name removes it, so "no name" is always "no key".
     *
     * @param type the kind of shop
     * @param name the name typed by the user; may be null or blank
     */
    public void setStoreName(@NonNull StoreType type, String name) {
        String clean = name == null ? "" : name.trim();
        SharedPreferences.Editor editor = prefs.edit();
        if (clean.isEmpty()) {
            editor.remove(KEY_STORE_PREFIX + type.getKey());
        } else {
            editor.putString(KEY_STORE_PREFIX + type.getKey(), clean);
        }
        // apply() writes in the background; commit() would block the main thread for a tiny value
        editor.apply();
    }

    /** @return true if the list is grouped by shop (the default) */
    public boolean isGroupByStore() {
        return prefs.getBoolean(KEY_GROUP_BY_STORE, true);
    }

    /** saves whether the list is grouped by shop */
    public void setGroupByStore(boolean grouped) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_GROUP_BY_STORE, grouped);
        editor.apply();
    }
}
