package cdp.t9;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {
    SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // initialize preference
        sp = getSharedPreferences("cdp.t9.settings", Activity.MODE_PRIVATE);

        // check if pin code is set
        if (sp.getString("pin_code", null) != null) {
            // proceed to pin code activity
        } else {
            // proceed to main activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
