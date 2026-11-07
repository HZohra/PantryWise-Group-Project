package com.cp470.groupproject.ui.shoppinglist;

import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;

/**
 * A one-shot message for the screen, described by resource ids so the view model needs no
 * context. the activity resolves it to text and shows it in a snackbar.
 */
public final class UiMessage {

    /** string or plurals resource to show */
    public final int resId;

    /** quantity used to pick the plural form, ignored when {@link #isPlural} is false */
    public final int quantity;

    /** format arguments of the text */
    public final Object[] args;

    /** true when {@link #resId} is a plurals resource */
    public final boolean isPlural;

    /** true when the snackbar should offer an undo action */
    public final boolean offerUndo;

    private UiMessage(int resId, int quantity, Object[] args, boolean isPlural, boolean offerUndo) {
        this.resId = resId;
        this.quantity = quantity;
        this.args = args;
        this.isPlural = isPlural;
        this.offerUndo = offerUndo;
    }

    /** @return a message made from a plain string resource */
    public static UiMessage of(@StringRes int resId, Object... args) {
        return new UiMessage(resId, 0, args, false, false);
    }

    /** @return a message made from a plurals resource; {@code quantity} also is the first argument */
    public static UiMessage plural(@PluralsRes int resId, int quantity) {
        return new UiMessage(resId, quantity, new Object[] {quantity}, true, false);
    }

    /** @return a message that offers to undo the last deletion */
    public static UiMessage undoable(@StringRes int resId) {
        return new UiMessage(resId, 0, new Object[0], false, true);
    }
}
