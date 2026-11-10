package com.cp470.groupproject.ui.ai;

import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.remote.AiError;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

/**
 * AI kitchen assistant chat screen (View in MVVM).
 * Only draws state from {@link AiChatViewModel} and forwards user actions to it.
 * Can be opened with a ready-made question (see {@link ChatLauncher}).
 */
public class AiAssistantActivity extends AppCompatActivity {

    private AiChatViewModel viewModel;
    private ChatMessageAdapter adapter;

    private View root;
    private View appBar;
    private View content;
    private View inputBar;
    private RecyclerView messageList;
    private TextView welcome;
    private LinearProgressIndicator progress;
    private TextInputEditText input;
    private MaterialButton sendButton;

    /** The error Snackbar currently on screen, so it can be dismissed when the user moves on. */
    @Nullable private Snackbar errorSnackbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_assistant);

        root = findViewById(R.id.ai_root);
        appBar = findViewById(R.id.ai_app_bar);
        content = findViewById(R.id.ai_content);
        inputBar = findViewById(R.id.ai_input_bar);
        messageList = findViewById(R.id.ai_messages);
        welcome = findViewById(R.id.ai_welcome);
        progress = findViewById(R.id.ai_progress);
        input = findViewById(R.id.ai_input);
        sendButton = findViewById(R.id.ai_send);

        MaterialToolbar toolbar = findViewById(R.id.ai_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        applyWindowInsets();

        adapter = new ChatMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // newest messages sit above the input, like a chat app
        messageList.setLayoutManager(layoutManager);
        messageList.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AiChatViewModel.class);
        viewModel.getMessages().observe(this, this::showMessages);
        viewModel.isLoading().observe(this, this::showLoading);
        viewModel.getError().observe(this, this::showError);

        sendButton.setOnClickListener(v -> onSendClicked());

        if (savedInstanceState == null) {
            applyDraft(); // only on first open: after rotation Android restores the box itself
        }
    }

    /**
     * Puts a question from another screen (e.g. "Ask assistant how to make this") in the input box.
     * It is NOT sent: the user reviews it, and the consent dialog still applies (US-6).
     */
    private void applyDraft() {
        String draft = getIntent().getStringExtra(ChatLauncher.EXTRA_DRAFT);
        if (draft == null || draft.trim().isEmpty()) {
            return;
        }
        input.setText(draft);
        Editable text = input.getText();
        if (text != null) {
            input.setSelection(text.length()); // cursor at the end, ready to add to it
        }
        input.requestFocus();
    }

    /** Keeps the toolbar below the status bar and the input bar above the navigation bar and keyboard. */
    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            appBar.setPadding(bars.left, bars.top, bars.right, 0);
            content.setPadding(bars.left, 0, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void showMessages(List<ChatMessage> list) {
        adapter.submitList(list, () -> {
            if (!list.isEmpty()) {
                messageList.scrollToPosition(list.size() - 1);
            }
        });
        welcome.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showLoading(Boolean isLoading) {
        boolean loading = Boolean.TRUE.equals(isLoading);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!loading);
    }

    /**
     * Retryable errors (no internet, timeout) stay on screen until the user taps Retry or moves on.
     * Rate limits and other errors disappear by themselves.
     */
    private void showError(@Nullable AiError error) {
        if (error == null) {
            return;
        }
        dismissError();
        boolean retryable = AiErrorText.isRetryable(error);
        errorSnackbar = Snackbar.make(root, AiErrorText.message(this, error),
                        retryable ? Snackbar.LENGTH_INDEFINITE : Snackbar.LENGTH_LONG)
                .setAnchorView(inputBar);
        if (retryable) {
            errorSnackbar.setAction(R.string.ai_retry, v -> viewModel.retry());
        }
        errorSnackbar.show();
        viewModel.onErrorShown();
    }

    private void dismissError() {
        if (errorSnackbar != null) {
            errorSnackbar.dismiss();
            errorSnackbar = null;
        }
    }

    private void onSendClicked() {
        Editable editable = input.getText();
        String text = editable == null ? "" : editable.toString().trim();
        if (text.isEmpty()) {
            return;
        }
        if (viewModel.needsConsent()) {
            showConsentDialog();
        } else {
            send(text);
        }
    }

    /** US-6: Continue sends; Cancel (or back) sends nothing and keeps the text in the box. */
    private void showConsentDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ai_consent_title)
                .setMessage(R.string.ai_consent_message)
                .setPositiveButton(R.string.ai_consent_continue, (dialog, which) -> {
                    viewModel.giveConsent();
                    onSendClicked();
                })
                .setNegativeButton(R.string.ai_consent_cancel, null)
                .show();
    }

    private void send(@NonNull String text) {
        dismissError();
        viewModel.sendMessage(text);
        input.setText(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ai_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_ai_new_chat) {
            dismissError();
            viewModel.newChat();
            Snackbar.make(root, R.string.ai_chat_cleared, Snackbar.LENGTH_SHORT)
                    .setAnchorView(inputBar)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Tahmina's InventoryActivity doesn't exist yet, so Up behaves like Back for now. */
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}