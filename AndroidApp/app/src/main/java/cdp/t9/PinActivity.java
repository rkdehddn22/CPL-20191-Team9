package cdp.t9;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PinActivity extends AppCompatActivity {
    Intent targetIntent;

    StringBuilder sb;
    String addr;

    TextView tvwInst;
    EditText edtAddr;

    Handler handler;
    Runnable rRunNextActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        getSupportActionBar().setTitle(R.string.pin_title);

        tvwInst = findViewById(R.id.tvw_pin_inst);
        edtAddr = findViewById(R.id.edt_pin_addr);

        edtAddr.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                CheckAddr(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        handler = new Handler();

        targetIntent = getIntent().getParcelableExtra("activity_to_launch");
        if (targetIntent == null) finish();

        addr = getIntent().getStringExtra("target_addr");

        sb = new StringBuilder();
        rRunNextActivity = new Runnable() {
            @Override
            public void run() {
                startActivity(targetIntent);
                finish();
            }
        };

        tvwInst.setText(R.string.pin_inst_enter);
    }

    private void CheckAddr(String current) {
        if (addr.equalsIgnoreCase(current)) handler.postDelayed(rRunNextActivity, 100);
    }

    /*
    private void EnterCode(int number) {
        if (sb.length() < 3) {
            sb.append(number);
            for (int i = 0; i < sb.length(); i++) {
                tvwCodes[i].setText(R.string.pin_code_fill);
            }
        } else if (sb.length() == 3) {
            sb.append(number);
            for (int i = 0; i < 4; i++) {
                tvwCodes[i].setText(R.string.pin_code_fill);
            }
            switch (mode) {
                case PIN_MODE_INPUT:
                    if (sb.toString().equals(sp.getString("pin_code", null))) {
                        handler.postDelayed(rRunNextActivity, 100);
                    } else {
                        for (int i = 0; i < 4; i++) {
                            tvwCodes[i].setText(R.string.pin_code_empty);
                        }
                        tvwInst.setText(R.string.pin_inst_invalid);
                        sb.setLength(0);
                    }
                    break;
                case PIN_MODE_CREATE:
                    if (confirm) {
                        if (sb.toString().equals(newPin)) {
                            // correct
                            sp.edit().putString("pin_code", newPin).apply();
                            finish();
                        } else {
                            newPin = null;
                            confirm = false;
                            sb.setLength(0);
                            for (int i = 0; i < 4; i++) {
                                tvwCodes[i].setText(R.string.pin_code_empty);
                            }
                            tvwInst.setText(R.string.pin_inst_create_confirm_invalid);
                        }
                    } else {
                        newPin = sb.toString();
                        confirm = true;
                        sb.setLength(0);
                        for (int i = 0; i < 4; i++) {
                            tvwCodes[i].setText(R.string.pin_code_empty);
                        }
                        tvwInst.setText(R.string.pin_inst_create_confirm);
                    }
                    break;
            }
        }
    }
    */
}
