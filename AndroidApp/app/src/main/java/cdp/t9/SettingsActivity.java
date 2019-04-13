package cdp.t9;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

public class SettingsActivity extends AppCompatPreferenceActivity {
    SharedPreferences sp;

    Preference pPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.settings_title);
        addPreferencesFromResource(R.xml.pref);

        sp = getSharedPreferences("cdp.t9.settings", Activity.MODE_PRIVATE);

        pPin = findPreference("pin");
        pPin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (sp.getString("pin_code", null) == null) {
                    Intent intent = new Intent(SettingsActivity.this, SettingsPinActivity.class);
                    startActivity(intent);
                } else {
                    Intent passIntent = new Intent(SettingsActivity.this, SettingsPinActivity.class);
                    Intent intent = new Intent(SettingsActivity.this, PinActivity.class);
                    intent.putExtra("mode", PinActivity.PIN_MODE_INPUT);
                    intent.putExtra("activity_to_launch", passIntent);
                    startActivity(intent);
                }
                return true;
            }
        });
    }
}
