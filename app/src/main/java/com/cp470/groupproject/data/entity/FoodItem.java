package com.cp470.groupproject.data.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.cp470.groupproject.data.model.ItemStatus;

import java.util.Objects;

/**
 * A household inventory food item.
 * Implements {@link Parcelable} to pass the complete object from InventoryActivity to
 * ItemDetailActivity with {@code Intent.putExtra(String, Parcelable)}.
 *
 * Dates are stored in milliseconds since 1970. A value of 0 in {@link #expirationDate} means
 * "no expiration date".
 */

@Entity(tableName = "food_items",
        indices = {@Index("categoryId"), @Index("storageLocationId"),
                @Index("expirationDate"), @Index("status"), @Index("normalizedName")})
public class FoodItem implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** name as entered by the user or returned by the scanner, e.g. "2% Milk 1L" */
    @NonNull
    public String name = "";

    /** canonical name for comparing with recipes, e.g. "milk". Calculated by the repository */
    @NonNull
    public String normalizedName = "";

    public double quantity;

    /** unit of measurement: "pcs", "g", "L", etc */
    @NonNull
    public String unit = "";

    /** category ID (categories table) */
    public long categoryId;

    /** storage location ID (storage_locations table) */
    public long storageLocationId;

    /** purchase date in milliseconds (0 = unknown) */
    public long purchaseDate;

    /** expiration date in milliseconds (0 = unknown) */
    public long expirationDate;

    /** one of the {@link ItemStatus} constants; ACTIVE while in inventory */
    @NonNull
    public String status = ItemStatus.ACTIVE;

    /** barcode if scanned; otherwise, null */
    @Nullable
    public String barcode;

    /** empty constructor required by Room */
    public FoodItem() { }

    /** convenience constructor for the UI, tests, and autofill */
    @Ignore
    public FoodItem(@NonNull String name, double quantity, @NonNull String unit,
                    long categoryId, long storageLocationId,
                    long purchaseDate, long expirationDate) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.categoryId = categoryId;
        this.storageLocationId = storageLocationId;
        this.purchaseDate = purchaseDate;
        this.expirationDate = expirationDate;
    }






    // ------------------------------------------------------------ Parcelable

    /** rebuilds the object from a Parcel. Order MUST match writeToParcel */
    @Ignore
    protected FoodItem(Parcel in) {
        id = in.readLong();
        name = Objects.requireNonNull(in.readString());
        normalizedName = Objects.requireNonNull(in.readString());
        quantity = in.readDouble();
        unit = Objects.requireNonNull(in.readString());
        categoryId = in.readLong();
        storageLocationId = in.readLong();
        purchaseDate = in.readLong();
        expirationDate = in.readLong();
        status = Objects.requireNonNull(in.readString());
        barcode = in.readString();
    }

    /** flattens the object into a Parcel. Order MUST match the Parcel constructor */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(normalizedName);
        dest.writeDouble(quantity);
        dest.writeString(unit);
        dest.writeLong(categoryId);
        dest.writeLong(storageLocationId);
        dest.writeLong(purchaseDate);
        dest.writeLong(expirationDate);
        dest.writeString(status);
        dest.writeString(barcode);
    }

    /** no file descriptors or special objects are packaged */
    @Override
    public int describeContents() {
        return 0;
    }

    /** static factory used by Android to recreate the object */
    public static final Creator<FoodItem> CREATOR = new Creator<FoodItem>() {
        @Override
        public FoodItem createFromParcel(Parcel in) {
            return new FoodItem(in);
        }

        @Override
        public FoodItem[] newArray(int size) {
            return new FoodItem[size];
        }
    };





    // ------------------------------------------------------- Object methods

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FoodItem)) return false;
        FoodItem f = (FoodItem) o;
        return id == f.id && Double.compare(quantity, f.quantity) == 0
                && categoryId == f.categoryId && storageLocationId == f.storageLocationId
                && purchaseDate == f.purchaseDate && expirationDate == f.expirationDate
                && name.equals(f.name) && normalizedName.equals(f.normalizedName)
                && unit.equals(f.unit) && status.equals(f.status)
                && Objects.equals(barcode, f.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, normalizedName, quantity, unit, categoryId,
                storageLocationId, purchaseDate, expirationDate, status, barcode);
    }

    @NonNull
    @Override
    public String toString() {
        return name + " (" + quantity + " " + unit + ")";
    }
}
