package com.cp470.groupproject.ui.ai;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * Opens the AI chat with a message already typed in the input box (not sent),
 * so the user can review it and the consent dialog still applies (US-6).
 */
public final class ChatLauncher {

    /** Read by AiAssistantActivity. */
    public static final String EXTRA_DRAFT = "com.cp470.groupproject.ui.ai.EXTRA_DRAFT";

    private ChatLauncher() { }

    @NonNull
    public static Intent openWithDraft(@NonNull Context context, @NonNull String draft) {
        return new Intent(context, AiAssistantActivity.class).putExtra(EXTRA_DRAFT, draft);
    }
}