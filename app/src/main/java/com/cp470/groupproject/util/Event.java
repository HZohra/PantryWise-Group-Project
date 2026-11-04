package com.cp470.groupproject.util;

/**
 * Wraps a value that must be consumed only once, such as a toast message.
 * it prevents livedata from delivering the same result again after a rotation.
 */
public class Event<T> {
    private final T content;
    private boolean handled = false;

    public Event(T content) {
        this.content = content;
    }

    /** @return the content the first time it is called, and null on later calls */
    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
}