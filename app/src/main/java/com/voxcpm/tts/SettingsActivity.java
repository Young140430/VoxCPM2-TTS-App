package com.voxcpm.tts;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

/**
 * Settings Activity - Configure API URL, Key, Model, Format.
 */
public class SettingsActivity extends AppCompatActivity {
    private EditText inputBaseUrl, inputApiKey, inputModel;
    private MaterialButtonToggleGroup formatToggle;
    private ImageButton btnToggleVisibility;
    private View btnHelp;
    private MaterialButton btnSave, btnClear;

    private String format = Constants.DEFAULT_FORMAT;
    private boolean showApiKey = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupClickListeners();
        loadSettings();
    }

    private void initViews() {
        inputBaseUrl = findViewById(R.id.input_base_url);
        inputApiKey = findViewById(R.id.input_api_key);
        inputModel = findViewById(R.id.input_model);
        formatToggle = findViewById(R.id.format_toggle);
        btnToggleVisibility = findViewById(R.id.btn_toggle_visibility);
        btnHelp = findViewById(R.id.btn_help);
        btnSave = findViewById(R.id.btn_save);
        btnClear = findViewById(R.id.btn_clear);
    }

    private void setupClickListeners() {
        btnToggleVisibility.setOnClickListener(v -> {
            showApiKey = !showApiKey;
            updateApiKeyVisibility();
        });

        btnHelp.setOnClickListener(v -> showHelpDialog());

        formatToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.format_wav) format = "wav";
                else if (checkedId == R.id.format_mp3) format = "mp3";
                else if (checkedId == R.id.format_flac) format = "flac";
                else if (checkedId == R.id.format_ogg) format = "ogg";
            }
        });

        btnSave.setOnClickListener(v -> saveSettings());
        btnClear.setOnClickListener(v -> clearSettings());
    }

    private void loadSettings() {
        SharedPreferences prefs = getPrefs();
        inputBaseUrl.setText(prefs.getString(Constants.PREF_API_BASE, Constants.DEFAULT_API_BASE));
        inputApiKey.setText(prefs.getString(Constants.PREF_API_KEY, Constants.DEFAULT_API_KEY));
        inputModel.setText(prefs.getString(Constants.PREF_MODEL, Constants.DEFAULT_MODEL));
        format = prefs.getString(Constants.PREF_FORMAT, Constants.DEFAULT_FORMAT);

        // Set format toggle
        switch (format) {
            case "wav": formatToggle.check(R.id.format_wav); break;
            case "mp3": formatToggle.check(R.id.format_mp3); break;
            case "flac": formatToggle.check(R.id.format_flac); break;
            case "ogg": formatToggle.check(R.id.format_ogg); break;
            default: formatToggle.check(R.id.format_wav); break;
        }

        updateApiKeyVisibility();
    }

    private void saveSettings() {
        String baseUrl = inputBaseUrl.getText().toString().trim();
        if (baseUrl.isEmpty()) {
            Toast.makeText(this, "请输入 API 地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // Auto-add http:// if no scheme
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "http://" + baseUrl;
            inputBaseUrl.setText(baseUrl);
        }

        getPrefs().edit()
                .putString(Constants.PREF_API_BASE, baseUrl)
                .putString(Constants.PREF_API_KEY, inputApiKey.getText().toString().trim())
                .putString(Constants.PREF_MODEL, inputModel.getText().toString().trim())
                .putString(Constants.PREF_FORMAT, format)
                .apply();

        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::finish, 800);
    }

    private void clearSettings() {
        new AlertDialog.Builder(this)
                .setTitle("确认清除")
                .setMessage("将清除所有已保存的设置")
                .setPositiveButton("确定", (dialog, which) -> {
                    getPrefs().edit()
                            .remove(Constants.PREF_API_BASE)
                            .remove(Constants.PREF_API_KEY)
                            .remove(Constants.PREF_MODEL)
                            .remove(Constants.PREF_FORMAT)
                            .apply();
                    loadSettings();
                    Toast.makeText(this, R.string.cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateApiKeyVisibility() {
        if (showApiKey) {
            inputApiKey.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            inputApiKey.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        }
        inputApiKey.setSelection(inputApiKey.getText().length());
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.help_title)
                .setMessage(getString(R.string.help_message))
                .setPositiveButton("知道了", null)
                .show();
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences("voxcpm_tts_prefs", MODE_PRIVATE);
    }
}
