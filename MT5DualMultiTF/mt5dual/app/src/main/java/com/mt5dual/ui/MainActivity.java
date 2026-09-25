package com.mt5dual.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.mt5dual.DualApplication;
import com.mt5dual.R;
import com.mt5dual.core.SignalStateManager;
import com.mt5dual.service.MonitoringForegroundService;
import com.mt5dual.settings.AppSettings;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_POST_NOTIFICATIONS = 501;

    private AppSettings appSettings;
    private MenuItem toggleMonitoringMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appSettings = new AppSettings(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.app_name);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = item.getItemId() == R.id.nav_dual_triggers
                    ? new DualTriggersFragment()
                    : new DualActiveSignalsFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment).commit();
            return true;
        });

        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_active_signals);
            showFirstRunBatteryPromptIfNeeded();
        }

        ensurePostNotificationPermission();

        if (appSettings.isMonitoringEnabled()) startMonitoringService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateToggleMonitoringTitle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        toggleMonitoringMenuItem = menu.findItem(R.id.action_toggle_monitoring);
        updateToggleMonitoringTitle();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_dual_window:
                showDualWindowDialog();
                return true;
            case R.id.action_repeat_interval:
                showRepeatIntervalDialog();
                return true;
            case R.id.action_price_difference:
                showPriceDifferenceDialog();
                return true;
            case R.id.action_permissions:
                showPermissionsDialog();
                return true;
            case R.id.action_toggle_monitoring:
                toggleMonitoring();
                return true;
            case R.id.action_about:
                showAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showDualWindowDialog() {
        EditText input = numericInput(String.valueOf(appSettings.getDualWindowCandles()));
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_dual_window)
                .setMessage(R.string.dual_window_description)
                .setView(input)
                .setPositiveButton(R.string.settings_save, (d, w) -> {
                    try {
                        int value = Integer.parseInt(input.getText().toString().trim());
                        if (value < 2) throw new NumberFormatException();
                        appSettings.setDualWindowCandles(value);
                        refreshVisibleFragment();
                    } catch (Exception ignored) { }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showRepeatIntervalDialog() {
        EditText input = numericInput(String.valueOf(appSettings.getRepeatIntervalMinutes()));
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_repeat_interval)
                .setMessage(R.string.repeat_interval_description)
                .setView(input)
                .setPositiveButton(R.string.settings_save, (d, w) -> {
                    try {
                        int value = Integer.parseInt(input.getText().toString().trim());
                        if (value < 1) throw new NumberFormatException();
                        appSettings.setRepeatIntervalMinutes(value);
                    } catch (Exception ignored) { }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showPriceDifferenceDialog() {
        String[] options = {
                getString(R.string.price_difference_on),
                getString(R.string.price_difference_off)
        };
        int checked = appSettings.isPriceDifferenceEnabled() ? 0 : 1;
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_price_difference)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    appSettings.setPriceDifferenceEnabled(which == 0);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private EditText numericInput(String current) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setText(current);
        input.setSelectAllOnFocus(true);
        return input;
    }

    private void showPermissionsDialog() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);

        addPermissionRow(root, R.string.permission_notification_access,
                notificationListenerEnabled() ? R.string.status_enabled : R.string.status_disabled,
                !notificationListenerEnabled(), v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addPermissionRow(root, R.string.permission_post_notifications,
                    hasPostNotificationPermission() ? R.string.status_enabled : R.string.status_disabled,
                    !hasPostNotificationPermission(), v -> ensurePostNotificationPermission());
        } else {
            addPermissionRow(root, R.string.permission_post_notifications,
                    R.string.status_not_required, false, null);
        }

        boolean battery = batteryOptimizationExempt();
        addPermissionRow(root, R.string.permission_battery,
                battery ? R.string.status_enabled : R.string.status_disabled,
                !battery, v -> requestIgnoreBatteryOptimizations());

        addPermissionRow(root, R.string.permission_monitoring_service,
                MonitoringForegroundService.isRunning() ? R.string.status_running : R.string.status_stopped,
                false, null);

        TextView note = new TextView(this);
        note.setText(R.string.permission_battery_note);
        root.addView(note, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_permissions)
                .setView(root)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void addPermissionRow(LinearLayout parent, int titleRes, int stateRes,
                                  boolean showButton, android.view.View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView text = new TextView(this);
        text.setText(getString(titleRes) + " — " + getString(stateRes));
        text.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text);

        if (showButton && listener != null) {
            Button button = new Button(this);
            button.setText(titleRes == R.string.permission_post_notifications
                    ? R.string.permission_request
                    : titleRes == R.string.permission_battery ? R.string.permission_request_exemption
                    : R.string.permission_open_settings);
            button.setOnClickListener(listener);
            row.addView(button);
        }
        parent.addView(row);
    }

    private void showFirstRunBatteryPromptIfNeeded() {
        if (batteryOptimizationExempt()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.permission_battery_title)
                .setMessage(R.string.permission_battery_first_run)
                .setPositiveButton(R.string.permission_request_exemption, (d, w) -> requestIgnoreBatteryOptimizations())
                .setNegativeButton(R.string.later, null)
                .show();
    }

    private void requestIgnoreBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null || pm.isIgnoringBatteryOptimizations(getPackageName())) return;
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void ensurePostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
        }
    }

    private boolean hasPostNotificationPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean notificationListenerEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(getPackageName());
    }

    private boolean batteryOptimizationExempt() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        return pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void startMonitoringService() {
        Intent intent = new Intent(this, MonitoringForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent);
        else startService(intent);
    }

    private void toggleMonitoring() {
        boolean newState = !appSettings.isMonitoringEnabled();
        appSettings.setMonitoringEnabled(newState);
        if (newState) startMonitoringService();
        else stopService(new Intent(this, MonitoringForegroundService.class));
        updateToggleMonitoringTitle();
    }

    private void updateToggleMonitoringTitle() {
        if (toggleMonitoringMenuItem == null) return;
        toggleMonitoringMenuItem.setTitle(appSettings != null && appSettings.isMonitoringEnabled()
                ? R.string.menu_stop_monitoring : R.string.menu_start_monitoring);
    }

    private void showAboutDialog() {
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = getString(R.string.unknown);
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_about)
                .setMessage(getString(R.string.about_description) + "\n"
                        + getString(R.string.about_version_label) + ": " + version)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void refreshVisibleFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (fragment instanceof DualActiveSignalsFragment) {
            ((DualActiveSignalsFragment) fragment).refresh();
        } else if (fragment instanceof DualTriggersFragment) {
            ((DualTriggersFragment) fragment).refresh();
        }
    }

    private int dp(int px) {
        return Math.round(px * getResources().getDisplayMetrics().density);
    }
}
