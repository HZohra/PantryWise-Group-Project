package com.cp470.groupproject.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

/** Reads the current value of a livedata inside a test. */
public final class LiveDataTestUtil {
    private LiveDataTestUtil() { }

    public static <T> T getValue(LiveData<T> liveData) {
        final Object[] holder = new Object[1];
        Observer<T> observer = t -> holder[0] = t;
        liveData.observeForever(observer);
        liveData.removeObserver(observer);
        @SuppressWarnings("unchecked") T value = (T) holder[0];
        return value;
    }
}