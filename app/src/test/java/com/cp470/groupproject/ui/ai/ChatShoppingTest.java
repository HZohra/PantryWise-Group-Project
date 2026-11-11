package com.cp470.groupproject.ui.ai;

import static org.junit.Assert.assertEquals;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.db.PantryDatabase;
import com.cp470.groupproject.data.remote.dto.ChatShoppingItemDto;
import com.cp470.groupproject.data.repository.AiRepository;
import com.cp470.groupproject.data.repository.PantryRepository;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

/** The chat's "Add N items?" card: Add saves once, No thanks saves nothing. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class ChatShoppingTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private PantryDatabase db;
    private PantryRepository pantry;
    private AiChatViewModel viewModel;
    private ChatMessage reply;

    @Before
    public void setUp() {
        Application app = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(app, PantryDatabase.class).allowMainThreadQueries().build();
        pantry = new PantryRepository(db, Runnable::run);
        viewModel = new AiChatViewModel(app, new AiRepository(app), pantry);
        reply = ChatMessage.fromAi("Confirm below.", Arrays.asList(
                new ChatShoppingItemDto("garlic", "2 cloves"),
                new ChatShoppingItemDto("olive oil", "")));
        viewModel.addMessageForTest(reply);
    }

    @After
    public void tearDown() {
        db.close();
    }

    private ChatMessage.ItemsState stateOnScreen() {
        List<ChatMessage> shown = viewModel.getMessages().getValue();
        return shown.get(shown.size() - 1).itemsState;
    }

    @Test
    public void add_savesTheItems_andMarksThemAdded() {
        final int[] added = new int[1];

        viewModel.addSuggestedItems(reply.id, count -> added[0] = count);

        assertEquals(2, added[0]);
        assertEquals(2, pantry.getShoppingItemsSync().size());
        assertEquals(ChatMessage.ItemsState.ADDED, stateOnScreen());
    }

    @Test
    public void addTwice_onlyAddsOnce() {
        viewModel.addSuggestedItems(reply.id, count -> { });
        viewModel.addSuggestedItems(reply.id, count -> { });

        assertEquals(2, pantry.getShoppingItemsSync().size());
    }

    @Test
    public void noThanks_savesNothing_andHidesTheCard() {
        viewModel.dismissSuggestedItems(reply.id);
        viewModel.addSuggestedItems(reply.id, count -> { });

        assertEquals(0, pantry.getShoppingItemsSync().size());
        assertEquals(ChatMessage.ItemsState.DISMISSED, stateOnScreen());
    }
}