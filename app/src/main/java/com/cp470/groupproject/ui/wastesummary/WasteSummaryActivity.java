package com.cp470.groupproject.ui.wastesummary;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.cp470.groupproject.R;
import com.cp470.groupproject.util.WindowInsetsHelper;
import com.cp470.groupproject.data.model.WasteStats;

/**
 * Activity that shows how many items were consumed, wasted, expired or donated
 * and the overall waste rate for a selected period.
 */
public class WasteSummaryActivity extends AppCompatActivity {

    /** look-back windows in days, same order as the period_labels array (0 = all time) */
    private static final int[] PERIOD_DAYS = {7, 30, 0};

    /** position of the period selected by default (last 30 days) */
    private static final int DEFAULT_PERIOD = 1;

    private WasteSummaryViewModel viewModel;

    private TextView txtEmpty;
    private TextView txtTotal;
    private TextView txtConsumed;
    private TextView txtWasted;
    private TextView txtExpired;
    private TextView txtDonated;
    private TextView txtWastePercent;
    private ProgressBar progressWaste;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waste_summary);
        WindowInsetsHelper.applySystemBarPadding(findViewById(R.id.root));

        // the app theme has no action bar, so the layout brings its own toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();

        viewModel = new ViewModelProvider(this)
                .get(WasteSummaryViewModel.class);

        configurePeriodSpinner(savedInstanceState == null);

        viewModel.getStats().observe(this, this::showStats);
    }

    /** initializes the views from the XML layout */
    private void initializeViews() {
        txtEmpty = findViewById(R.id.txtEmpty);
        txtTotal = findViewById(R.id.txtTotal);
        txtConsumed = findViewById(R.id.txtConsumed);
        txtWasted = findViewById(R.id.txtWasted);
        txtExpired = findViewById(R.id.txtExpired);
        txtDonated = findViewById(R.id.txtDonated);
        txtWastePercent = findViewById(R.id.txtWastePercent);
        progressWaste = findViewById(R.id.progressWaste);
    }

    /** fills the period spinner and sends the selected period to the viewmodel */
    private void configurePeriodSpinner(boolean selectDefault) {
        Spinner spinnerPeriod = findViewById(R.id.spinnerPeriod);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.period_labels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setPeriodDays(PERIOD_DAYS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nothing to do
            }
        });

        if (selectDefault) {
            spinnerPeriod.setSelection(DEFAULT_PERIOD);
        }
    }

    /** displays the statistics on the screen */
    private void showStats(WasteStats stats) {
        if (stats == null) return;

        txtEmpty.setVisibility(stats.totalCount == 0 ? View.VISIBLE : View.GONE);

        txtTotal.setText(getString(R.string.waste_total, stats.totalCount));
        txtConsumed.setText(getString(R.string.waste_consumed, stats.consumedCount));
        txtWasted.setText(getString(R.string.waste_wasted, stats.wastedCount));
        txtExpired.setText(getString(R.string.waste_expired, stats.expiredCount));
        txtDonated.setText(getString(R.string.waste_donated, stats.donatedCount));
        txtWastePercent.setText(getString(R.string.waste_percent, stats.getWastePercent()));

        progressWaste.setProgress((int) Math.round(stats.getWastePercent()));
    }

    /** the up arrow just closes the screen: the main screen is already below it in the back stack */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
