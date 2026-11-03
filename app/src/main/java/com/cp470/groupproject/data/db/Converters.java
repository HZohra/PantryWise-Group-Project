package com.cp470.groupproject.data.db;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Room type converters. */

public final class Converters {
    /** separator that cannot appear in normal ingredient text */
    private static final String SEPARATOR = "\n";

    private Converters() { }

    /** joins a string list into a single column value */
    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(SEPARATOR);
            sb.append(list.get(i));
        }
        return sb.toString();
    }

    /** splits a column value back into a list */
    @TypeConverter
    public static List<String> toList(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(value.split(SEPARATOR)));
    }
}