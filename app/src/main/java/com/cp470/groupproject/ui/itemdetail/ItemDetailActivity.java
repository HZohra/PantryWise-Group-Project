package com.cp470.groupproject.ui.itemdetail;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.model.ItemStatus;

/**
 * Activity for displaying and managing the details of a food item.
 * receives a parcelable fooditem through an explicit intent and delegates
 * database operations to the viewmodel.
 */
public class ItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_ITEM = "extra_food_item";

    private ItemDetailViewModel viewModel;

    private TextView txtItemName;
    private EditText edtQuantity;

    private Button btnUpdate;
    private Button btnConsumed;
    private Button btnWasted;
    private Button btnExpired;
    private Button btnDonated;
    private Button btnDelete;

    /** creates an explicit intent containing the selected food item */
    public static Intent newIntent(Context context, FoodItem item) {
        Intent intent = new Intent(context, ItemDetailActivity.class);
        intent.putExtra(EXTRA_FOOD_ITEM, item);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // shows the up arrow, it returns to the parent declared in the manifest
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_item_detail);
        }

        initializeViews();

        viewModel = new ViewModelProvider(this)
                .get(ItemDetailViewModel.class);

        FoodItem receivedItem = getReceivedItem();

        if (receivedItem == null) {
            Toast.makeText(this, R.string.msg_load_error,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        observeViewModel();
        viewModel.loadItem(receivedItem.id);

        configureButtons();
    }

    /** initializes the views from the XML layout */
    private void initializeViews() {
        txtItemName = findViewById(R.id.txtItemName);
        edtQuantity = findViewById(R.id.edtQuantity);

        btnUpdate = findViewById(R.id.btnUpdate);
        btnConsumed = findViewById(R.id.btnConsumed);
        btnWasted = findViewById(R.id.btnWasted);
        btnExpired = findViewById(R.id.btnExpired);
        btnDonated = findViewById(R.id.btnDonated);
        btnDelete = findViewById(R.id.btnDelete);
    }

    /** retrieves the parcelable food item passed by the previous activity */
    private FoodItem getReceivedItem() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return getIntent().getParcelableExtra(
                    EXTRA_FOOD_ITEM, FoodItem.class);
        }

        return getIntent().getParcelableExtra(EXTRA_FOOD_ITEM);
    }

    /** observes changes in the item and database operation results */
    private void observeViewModel() {

        viewModel.getItem().observe(this, item -> {
            if (item != null) {
                txtItemName.setText(item.name);
                edtQuantity.setText(String.valueOf(item.quantity));
            }
        });

        viewModel.getOperationResult().observe(this, event -> {
            ItemDetailViewModel.UiResult result = event.getContentIfNotHandled();
            if (result == null) return;

            Toast.makeText(this, result.messageRes, Toast.LENGTH_SHORT).show();

            if (result.closeScreen) {
                finish();
            }
        });
    }

    /** configures the buttons and connects them to viewmodel operations */
    private void configureButtons() {

        btnUpdate.setOnClickListener(v -> {
            String quantityText = edtQuantity.getText().toString().trim();

            if (quantityText.isEmpty()) {
                edtQuantity.setError(getString(R.string.error_quantity_required));
                return;
            }

            try {
                double quantity = Double.parseDouble(quantityText);

                if (quantity <= 0) {
                    edtQuantity.setError(getString(R.string.error_quantity_positive));
                    return;
                }

                viewModel.updateQuantity(quantity);

            } catch (NumberFormatException e) {
                edtQuantity.setError(getString(R.string.error_quantity_invalid));
            }
        });

        btnConsumed.setOnClickListener(v ->
                viewModel.recordDisposition(ItemStatus.CONSUMED));

        btnWasted.setOnClickListener(v ->
                viewModel.recordDisposition(ItemStatus.WASTED));

        btnExpired.setOnClickListener(v ->
                viewModel.recordDisposition(ItemStatus.EXPIRED));

        btnDonated.setOnClickListener(v ->
                viewModel.recordDisposition(ItemStatus.DONATED));

        btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    /** asks for confirmation before permanently deleting the item */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) ->
                        viewModel.deleteItem())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}