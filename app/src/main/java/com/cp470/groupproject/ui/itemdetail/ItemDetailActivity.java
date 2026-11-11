package com.cp470.groupproject.ui.itemdetail;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.cp470.groupproject.R;
import com.cp470.groupproject.util.WindowInsetsHelper;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.entity.StorageLocation;
import com.cp470.groupproject.data.model.ItemStatus;
import com.cp470.groupproject.ui.settings.DisplayPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FOOD_ITEM = "extra_food_item";

    private ItemDetailViewModel viewModel;

    private TextView txtItemName;
    private TextView txtExpirationDate;
    private EditText edtName;
    private EditText edtQuantity;
    private EditText edtUnit;
    private Spinner spinnerLocation;

    private Button btnUpdate;
    private Button btnConsumed;
    private Button btnWasted;
    private Button btnExpired;
    private Button btnDonated;
    private Button btnDelete;

    private long expirationDate = 0;

    public static Intent newIntent(Context context, FoodItem item) {
        Intent intent = new Intent(context, ItemDetailActivity.class);
        intent.putExtra(EXTRA_FOOD_ITEM, item);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        WindowInsetsHelper.applySystemBarPadding(findViewById(R.id.root));
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        DisplayPreferences.applyTextSize(this);

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

        setupLocationSpinner();
        observeViewModel();
        viewModel.loadItem(receivedItem.id);
        configureButtons();
    }

    private void initializeViews() {
        txtItemName = findViewById(R.id.txtItemName);
        txtExpirationDate = findViewById(R.id.txtExpirationDate);
        edtName = findViewById(R.id.edtName);
        edtQuantity = findViewById(R.id.edtQuantity);
        edtUnit = findViewById(R.id.edtUnit);
        spinnerLocation = findViewById(R.id.spinnerLocation);

        btnUpdate = findViewById(R.id.btnUpdate);
        btnConsumed = findViewById(R.id.btnConsumed);
        btnWasted = findViewById(R.id.btnWasted);
        btnExpired = findViewById(R.id.btnExpired);
        btnDonated = findViewById(R.id.btnDonated);
        btnDelete = findViewById(R.id.btnDelete);
    }

    private FoodItem getReceivedItem() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return getIntent().getParcelableExtra(
                    EXTRA_FOOD_ITEM, FoodItem.class);
        }

        return getIntent().getParcelableExtra(EXTRA_FOOD_ITEM);
    }

    private void setupLocationSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.storage_locations,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);

        spinnerLocation.setAdapter(adapter);
    }

    private void observeViewModel() {

        viewModel.getItem().observe(this, item -> {
            if (item != null) {
                txtItemName.setText(item.name);
                edtName.setText(item.name);
                edtQuantity.setText(String.valueOf(item.quantity));
                edtUnit.setText(item.unit);

                if (item.storageLocationId == StorageLocation.FREEZER_ID) {
                    spinnerLocation.setSelection(1);
                } else if (item.storageLocationId == StorageLocation.PANTRY_ID) {
                    spinnerLocation.setSelection(2);
                } else {
                    spinnerLocation.setSelection(0);
                }

                expirationDate = item.expirationDate;

                if (expirationDate > 0) {
                    SimpleDateFormat format =
                            new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    txtExpirationDate.setText(
                            format.format(expirationDate));
                } else {
                    txtExpirationDate.setText(R.string.form_no_expiration_selected);
                }
            }
        });

        viewModel.getOperationResult().observe(this, event -> {
            ItemDetailViewModel.UiResult result =
                    event.getContentIfNotHandled();

            if (result == null) return;

            Toast.makeText(this, result.messageRes,
                    Toast.LENGTH_SHORT).show();

            if (result.closeScreen) {
                finish();
            }
        });
    }

    private void configureButtons() {

        btnUpdate.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            String quantityText = edtQuantity.getText().toString().trim();
            String unit = edtUnit.getText().toString().trim();

            if (name.isEmpty()) {
                edtName.setError(getString(R.string.form_error_name));
                return;
            }

            if (quantityText.isEmpty()) {
                edtQuantity.setError(
                        getString(R.string.error_quantity_required));
                return;
            }

            if (unit.isEmpty()) {
                edtUnit.setError(getString(R.string.form_error_unit));
                return;
            }

            try {
                double quantity = Double.parseDouble(quantityText);

                if (quantity <= 0) {
                    edtQuantity.setError(
                            getString(R.string.error_quantity_positive));
                    return;
                }

                long locationId;

                switch (spinnerLocation.getSelectedItemPosition()) {
                    case 1:
                        locationId = StorageLocation.FREEZER_ID;
                        break;
                    case 2:
                        locationId = StorageLocation.PANTRY_ID;
                        break;
                    default:
                        locationId = StorageLocation.FRIDGE_ID;
                        break;
                }

                viewModel.updateFoodItem(
                        name,
                        quantity,
                        unit,
                        locationId,
                        expirationDate
                );

            } catch (NumberFormatException e) {
                edtQuantity.setError(
                        getString(R.string.error_quantity_invalid));
            }
        });

        findViewById(R.id.btnSelectExpirationDate).setOnClickListener(
                v -> showDatePicker());

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

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        if (expirationDate > 0) {
            calendar.setTimeInMillis(expirationDate);
        }

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);

                    expirationDate = selectedDate.getTimeInMillis();

                    SimpleDateFormat format =
                            new SimpleDateFormat("MMM dd, yyyy",
                                    Locale.getDefault());

                    txtExpirationDate.setText(
                            format.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.show();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(R.string.dialog_delete_message)
                .setPositiveButton(R.string.action_delete,
                        (dialog, which) -> viewModel.deleteItem())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}