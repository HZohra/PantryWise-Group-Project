package com.cp470.groupproject.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.cp470.groupproject.data.entity.FoodItem;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/** Checks that the parcelable implementation round-trips every field. */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 34)
public class FoodItemParcelTest {

    @Test
    public void roundTripKeepsAllFields() {
        FoodItem original = new FoodItem("2% Milk 1L", 2.5, "L", 1, 1, 1000L, 2000L);
        original.id = 42;
        original.normalizedName = "milk";
        original.barcode = "0123456789012";

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        FoodItem copy = FoodItem.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertEquals(original, copy);
    }

    @Test
    public void nullBarcodeSurvives() {
        FoodItem original = new FoodItem("Apple", 1, "pcs", 2, 3, 0, 0);

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        FoodItem copy = FoodItem.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertNull(copy.barcode);
        assertEquals(0, original.describeContents());
    }

    @Test
    public void creatorMakesArrays() {
        assertEquals(3, FoodItem.CREATOR.newArray(3).length);
    }
}