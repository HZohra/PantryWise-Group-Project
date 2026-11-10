package com.cp470.groupproject.ui.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;

/**
 * Shows the chat as bubbles: user messages on the right, assistant replies on the left.
 * The Activity only calls {@link #submitList}; DiffUtil works out what changed.
 */
public class ChatMessageAdapter extends ListAdapter<ChatMessage, ChatMessageAdapter.MessageViewHolder> {

    static final int TYPE_USER = 0;
    static final int TYPE_AI = 1;

    /** Package-private so the unit test can check it. */
    static final DiffUtil.ItemCallback<ChatMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatMessage oldItem, @NonNull ChatMessage newItem) {
            return oldItem.hasSameContentAs(newItem);
        }
    };

    public ChatMessageAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isFromUser() ? TYPE_USER : TYPE_AI;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_USER ? R.layout.item_chat_user : R.layout.item_chat_ai;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    /** One ViewHolder for both bubble types: both layouts use the id ai_message_text. */
    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.ai_message_text);
        }

        void bind(@NonNull ChatMessage message) {
            Context context = itemView.getContext();
            textView.setText(message.text);
            String sender = context.getString(message.isFromUser()
                    ? R.string.ai_chat_you
                    : R.string.ai_chat_assistant);
            // TalkBack reads "You: …" / "Assistant: …"
            textView.setContentDescription(
                    context.getString(R.string.ai_chat_message_a11y, sender, message.text));
        }
    }
}