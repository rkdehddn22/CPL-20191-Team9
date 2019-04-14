package cdp.t9;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {
    private final int REQUEST_PERM = 1;

    SharedPreferences sp;

    Handler handler;
    Runnable rStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /*
        // initialize preference
        sp = getSharedPreferences("cdp.t9.settings", Activity.MODE_PRIVATE);

        handler = new Handler();
        rStart = new Runnable() {
            @Override
            public void run() {
                // check if pin code is set
                if (sp.getString("pin_code", null) != null) {
                    // proceed to pin code activity
                    Intent intPin = new Intent(SplashActivity.this, PinActivity.class);
                    intPin.putExtra("mode", PinActivity.PIN_MODE_INPUT);
                    intPin.putExtra("activity_to_launch", new Intent(SplashActivity.this, MainActivity.class));
                    startActivity(intPin);
                    finish();
                } else {
                    // proceed to main activity
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                }
            }
        };

        */

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    REQUEST_PERM);
        } else {
            //handler.postDelayed(rStart, 500);
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //handler.postDelayed(rStart, 500);
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            } else {
                finish();
            }
        }
    }
}
