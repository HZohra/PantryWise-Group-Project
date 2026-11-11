package com.cp470.groupproject.ui.ai;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cp470.groupproject.R;

/**
 * Shows the chat as bubbles: user messages on the right, assistant replies on the left.
 * Under a reply that proposes shopping-list items, a card asks "Add N items…?" [No thanks] [Add].
 * The Activity only calls {@link #submitList}; DiffUtil works out what changed.
 */
public class ChatMessageAdapter extends ListAdapter<ChatMessage, ChatMessageAdapter.MessageViewHolder> {

    static final int TYPE_USER = 0;
    static final int TYPE_AI = 1;

    /** Taps on the shopping-list card. */
    public interface ItemsListener {
        void onAddItems(@NonNull ChatMessage message);
        void onDismissItems(@NonNull ChatMessage message);
    }

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

    @Nullable private final ItemsListener listener;

    public ChatMessageAdapter() {
        this(null);
    }

    public ChatMessageAdapter(@Nullable ItemsListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
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
        holder.bind(getItem(position), listener);
    }

    /** One ViewHolder for both bubble types; only the assistant layout has the items card. */
    static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView textView;
        @Nullable private final View itemsCard;
        @Nullable private final TextView itemsTitle;
        @Nullable private final TextView itemsList;
        @Nullable private final View itemsButtons;
        @Nullable private final View itemsAdd;
        @Nullable private final View itemsNo;
        @Nullable private final View itemsDone;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.ai_message_text);
            itemsCard = itemView.findViewById(R.id.ai_items_card);
            itemsTitle = itemView.findViewById(R.id.ai_items_title);
            itemsList = itemView.findViewById(R.id.ai_items_list);
            itemsButtons = itemView.findViewById(R.id.ai_items_buttons);
            itemsAdd = itemView.findViewById(R.id.ai_items_add);
            itemsNo = itemView.findViewById(R.id.ai_items_no);
            itemsDone = itemView.findViewById(R.id.ai_items_done);
        }

        void bind(@NonNull ChatMessage message, @Nullable ItemsListener listener) {
            Context context = itemView.getContext();
            textView.setText(message.text);
            String sender = context.getString(message.isFromUser()
                    ? R.string.ai_chat_you
                    : R.string.ai_chat_assistant);
            // TalkBack reads "You: …" / "Assistant: …"
            textView.setContentDescription(
                    context.getString(R.string.ai_chat_message_a11y, sender, message.text));
            bindItems(message, listener);
        }

        private void bindItems(@NonNull ChatMessage message, @Nullable ItemsListener listener) {
            if (itemsCard == null || itemsTitle == null || itemsList == null
                    || itemsButtons == null || itemsAdd == null || itemsNo == null || itemsDone == null) {
                return; // user bubble
            }
            boolean pending = message.itemsState == ChatMessage.ItemsState.PENDING;
            boolean added = message.itemsState == ChatMessage.ItemsState.ADDED;
            if (!pending && !added) {
                itemsCard.setVisibility(View.GONE);
                return;
            }
            Context context = itemView.getContext();
            int count = message.items.size();
            itemsCard.setVisibility(View.VISIBLE);
            itemsTitle.setText(context.getResources().getQuantityString(R.plurals.ai_chat_items_title, count, count));
            itemsTitle.setVisibility(pending ? View.VISIBLE : View.GONE);
            itemsList.setText(message.itemsText());
            itemsButtons.setVisibility(pending ? View.VISIBLE : View.GONE);
            itemsDone.setVisibility(added ? View.VISIBLE : View.GONE);
            itemsAdd.setOnClickListener(pending && listener != null ? v -> listener.onAddItems(message) : null);
            itemsNo.setOnClickListener(pending && listener != null ? v -> listener.onDismissItems(message) : null);
        }
    }
}