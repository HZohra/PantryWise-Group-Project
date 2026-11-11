package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.cp470.groupproject.data.remote.dto.ChatMessageDto;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AiChatViewModelTest {

    @Test
    public void toHistory_emptyConversation_isEmpty() {
        assertTrue(AiChatViewModel.toHistory(Collections.emptyList()).isEmpty());
    }

    @Test
    public void toHistory_mapsRolesInOrder() {
        List<ChatMessageDto> history = AiChatViewModel.toHistory(Arrays.asList(
                ChatMessage.fromUser("What can I cook?"),
                ChatMessage.fromAi("Try an omelette."),
                ChatMessage.fromUser("Make it vegetarian"),
                ChatMessage.fromAi("Use spinach instead of ham.")));

        assertEquals(4, history.size());
        assertEquals(ChatMessageDto.ROLE_USER, history.get(0).role);
        assertEquals("What can I cook?", history.get(0).content);
        assertEquals(ChatMessageDto.ROLE_ASSISTANT, history.get(1).role);
        assertEquals(ChatMessageDto.ROLE_ASSISTANT, history.get(3).role);
    }

    @Test
    public void toHistory_skipsUnansweredUserMessages() {
        List<ChatMessageDto> history = AiChatViewModel.toHistory(Arrays.asList(
                ChatMessage.fromUser("failed one"),   // no reply: skipped
                ChatMessage.fromUser("Hi"),
                ChatMessage.fromAi("Hello!"),
                ChatMessage.fromUser("also failed"))); // last and unanswered: skipped

        assertEquals(2, history.size());
        assertEquals("Hi", history.get(0).content);
        assertEquals("Hello!", history.get(1).content);
    }
}