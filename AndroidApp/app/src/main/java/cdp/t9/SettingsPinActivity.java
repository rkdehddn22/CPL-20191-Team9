package cdp.t9;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsPinActivity extends AppCompatPreferenceActivity {
    SharedPreferences sp;

    Preference pNewCreate;
    Preference pExistClear;
    Preference pExistChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(R.string.settings_security_pin);
        sp = getSharedPreferences("cdp.t9.settings", Activity.MODE_PRIVATE);

        if (sp.getString("pin_code", null) == null) {
            addPreferencesFromResource(R.xml.pref_pin_new);
            pNewCreate = findPreference("pin_create");
            pNewCreate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(SettingsPinActivity.this, PinActivity.class);
                    //intent.putExtra("mode", PinActivity.PIN_MODE_CREATE);
                    startActivity(intent);
                    finish();
                    return true;
                }
            });
        } else {
            addPreferencesFromResource(R.xml.pref_pin_exist);
            pExistClear = findPreference("pin_clear");
            pExistClear.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    sp.edit().remove("pin_code").apply();
                    finish();
                    return true;
                }
            });
            pExistChange = findPreference("pin_change");
            pExistChange.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(SettingsPinActivity.this, PinActivity.class);
                    //intent.putExtra("mode", PinActivity.PIN_MODE_CREATE);
                    startActivity(intent);
                    finish();
                    return true;
                }
            });
        }
    }
}
