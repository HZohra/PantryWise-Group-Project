package com.cp470.groupproject.shopping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.cp470.groupproject.util.StoreType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

/** Tests the settings of the shopping list with a mocked SharedPreferences. */
@RunWith(MockitoJUnitRunner.class)
public class StorePreferencesTest {

    @Mock
    private SharedPreferences prefs;
    @Mock
    private SharedPreferences.Editor editor;

    private StorePreferences stores;

    @Before
    public void setUp() {
        stores = new StorePreferences(prefs);
    }

    @Test
    public void getStoreNames_nothingSaved_isEmpty() {
        assertTrue(stores.getStoreNames().isEmpty());
    }

    @Test
    public void getStoreNames_onlyReturnsSavedTypes() {
        when(prefs.getString("store_name_butcher", null)).thenReturn("Carnes Lopez");

        Map<StoreType, String> names = stores.getStoreNames();

        assertEquals(1, names.size());
        assertEquals("Carnes Lopez", names.get(StoreType.BUTCHER));
    }

    @Test
    public void setStoreName_trimsAndSaves() {
        when(prefs.edit()).thenReturn(editor);

        stores.setStoreName(StoreType.BAKERY, "  Panes Ana  ");

        verify(editor).putString("store_name_bakery", "Panes Ana");
        verify(editor).apply();
    }

    @Test
    public void setStoreName_blank_removesTheKey() {
        when(prefs.edit()).thenReturn(editor);

        stores.setStoreName(StoreType.FISH, "   ");

        verify(editor).remove("store_name_fish");
        verify(editor, never()).putString(anyString(), any());
        verify(editor).apply();
    }

    @Test
    public void setStoreName_null_removesTheKey() {
        when(prefs.edit()).thenReturn(editor);

        stores.setStoreName(StoreType.PRODUCE, null);

        verify(editor).remove("store_name_produce");
    }

    @Test
    public void setStoreName_usesApplyNotCommit() {
        when(prefs.edit()).thenReturn(editor);

        stores.setStoreName(StoreType.GENERAL, "Mercadona");

        verify(editor).apply();
        verify(editor, never()).commit();
    }

    @Test
    public void isGroupByStore_nothingSaved_defaultsToTrue() {
        when(prefs.getBoolean("group_by_store", true)).thenReturn(true);

        assertTrue(stores.isGroupByStore());
    }

    @Test
    public void isGroupByStore_savedFalse_isFalse() {
        when(prefs.getBoolean("group_by_store", true)).thenReturn(false);

        assertFalse(stores.isGroupByStore());
    }

    @Test
    public void setGroupByStore_false_isSaved() {
        when(prefs.edit()).thenReturn(editor);

        stores.setGroupByStore(false);

        verify(editor).putBoolean("group_by_store", false);
        verify(editor).apply();
    }
}
