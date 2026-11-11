package com.cp470.groupproject.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.cp470.groupproject.R;
import com.cp470.groupproject.util.WindowInsetsHelper;
import com.cp470.groupproject.ui.settings.DisplayPreferences;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private RadioGroup themeOptions;
    private SeekBar textSizeSlider;
    private TextView textSizeValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        WindowInsetsHelper.applySystemBarPadding(findViewById(R.id.root));
        DisplayPreferences.applyTextSize(this);

        preferences = getSharedPreferences("PantryPreferences", MODE_PRIVATE);

        themeOptions = findViewById(R.id.themeOptions);
        textSizeSlider = findViewById(R.id.textSizeSlider);
        textSizeValue = findViewById(R.id.textSizeValue);

        textSizeSlider.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        textSizeValue.setText(getString(R.string.settings_text_size_value, progress));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
        loadPreferences();

        Button saveButton = findViewById(R.id.saveSettingsButton);
        saveButton.setOnClickListener(v -> savePreferences());

        Button backButton = findViewById(R.id.settingsBackButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void loadPreferences() {
        String theme = preferences.getString("theme", "system");
        int textSize = preferences.getInt("text_size", 16);

        switch (theme) {
            case "light":
                themeOptions.check(R.id.themeLight);
                break;
            case "dark":
                themeOptions.check(R.id.themeDark);
                break;
            default:
                themeOptions.check(R.id.themeSystem);
                break;
        }

        textSizeSlider.setProgress(textSize);
        textSizeValue.setText(getString(R.string.settings_text_size_value, textSize));
    }

    private void savePreferences() {
        String theme = "system";

        int selectedTheme = themeOptions.getCheckedRadioButtonId();

        if (selectedTheme == R.id.themeLight) {
            theme = "light";
        } else if (selectedTheme == R.id.themeDark) {
            theme = "dark";
        }

        int textSize = textSizeSlider.getProgress();

        preferences.edit()
                .putString("theme", theme)
                .putInt("text_size", textSize)
                .apply();

        applyTheme(theme);

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void applyTheme(String theme) {
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}