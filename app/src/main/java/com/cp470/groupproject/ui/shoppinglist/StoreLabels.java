package com.cp470.groupproject.ui.shoppinglist;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.cp470.groupproject.R;
import com.cp470.groupproject.util.StoreType;

/**
 * Maps a {@link StoreType} to its texts. the enum is plain java and knows nothing about android
 * resources, so the screen resolves them here.
 */
final class StoreLabels {

    private StoreLabels() { }

    /** @return the string that names this kind of shop ("Butcher") */
    @StringRes
    static int nameRes(@NonNull StoreType type) {
        switch (type) {
            case BUTCHER:
                return R.string.store_type_butcher;
            case FISH:
                return R.string.store_type_fish;
            case PRODUCE:
                return R.string.store_type_produce;
            case BAKERY:
                return R.string.store_type_bakery;
            default:
                return R.string.store_type_general;
        }
    }

    /** @return the string to look for in a maps app for this kind of shop ("butcher") */
    @StringRes
    static int queryRes(@NonNull StoreType type) {
        switch (type) {
            case BUTCHER:
                return R.string.store_query_butcher;
            case FISH:
                return R.string.store_query_fish;
            case PRODUCE:
                return R.string.store_query_produce;
            case BAKERY:
                return R.string.store_query_bakery;
            default:
                return R.string.store_query_general;
        }
    }
}
