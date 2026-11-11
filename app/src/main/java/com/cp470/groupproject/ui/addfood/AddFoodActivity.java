package com.cp470.groupproject.ui.addfood;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ExperimentalGetImage;

import com.cp470.groupproject.R;
import com.cp470.groupproject.data.entity.FoodItem;
import com.cp470.groupproject.data.entity.StorageLocation;
import com.cp470.groupproject.data.repository.PantryRepository;
import com.cp470.groupproject.scanner.ScannerActivity;
import com.cp470.groupproject.ui.settings.DisplayPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddFoodActivity extends AppCompatActivity {

    private PantryRepository repository;

    private EditText foodName;
    private EditText foodQuantity;
    private EditText foodUnit;
    private Spinner foodLocation;
    private TextView expirationDateText;

    private long expirationDate = 0;

    /** the scanner screen is marked experimental by camerax, so this opts in to launch it */
    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);
        DisplayPreferences.applyTextSize(this);

        repository = new PantryRepository(getApplication());

        foodName = findViewById(R.id.foodNameInput);
        foodQuantity = findViewById(R.id.foodQuantityInput);
        foodUnit = findViewById(R.id.foodUnitInput);
        foodLocation = findViewById(R.id.foodLocationSpinner);
        expirationDateText = findViewById(R.id.expirationDateText);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.storage_locations,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        foodLocation.setAdapter(adapter);

        Button dateButton = findViewById(R.id.selectExpirationDateButton);
        dateButton.setOnClickListener(v -> showDatePicker());

        Button saveButton = findViewById(R.id.saveFoodButton);
        saveButton.setOnClickListener(v -> saveFood());

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // the scanner returns the product name, which fills the name field
        ActivityResultLauncher<Intent> scanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String name = result.getData()
                                .getStringExtra(ScannerActivity.EXTRA_PRODUCT_NAME);
                        if (name != null) {
                            foodName.setText(name);
                        }
                    }
                });

        findViewById(R.id.scanBarcodeButton).setOnClickListener(v ->
                scanLauncher.launch(new Intent(this, ScannerActivity.class)));
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);

                    expirationDate = selectedDate.getTimeInMillis();

                    SimpleDateFormat format = new SimpleDateFormat(
                            "MMM dd, yyyy",
                            Locale.getDefault()
                    );

                    expirationDateText.setText(
                            format.format(selectedDate.getTime())
                    );
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePicker.getDatePicker().setMinDate(
                System.currentTimeMillis()
        );

        datePicker.show();
    }

    private void saveFood() {
        String name = foodName.getText().toString().trim();
        String quantityText = foodQuantity.getText().toString().trim();
        String unit = foodUnit.getText().toString().trim();

        if (name.isEmpty() || quantityText.isEmpty() || unit.isEmpty()) {
            Toast.makeText(this, R.string.add_food_fill_all,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        double quantity;

        try {
            quantity = Double.parseDouble(quantityText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.add_food_invalid_quantity,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (quantity <= 0) {
            Toast.makeText(this, R.string.add_food_quantity_positive,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        long locationId;

        switch (foodLocation.getSelectedItemPosition()) {
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

        FoodItem item = new FoodItem(
                name,
                quantity,
                unit,
                1,
                locationId,
                System.currentTimeMillis(),
                expirationDate
        );

        repository.insertFoodItem(item, id -> runOnUiThread(() -> {
            Toast.makeText(this, R.string.add_food_saved,
                    Toast.LENGTH_SHORT).show();
            finish();
        }));
    }
}