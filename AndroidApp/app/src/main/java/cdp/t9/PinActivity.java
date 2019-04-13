package cdp.t9;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PinActivity extends AppCompatActivity {
    public static final int PIN_MODE_INPUT = 0;
    public static final int PIN_MODE_CREATE = 1;

    SharedPreferences sp;
    Intent targetIntent;

    int mode;
    boolean confirm = false;

    StringBuilder sb;
    String newPin;

    Button[] btnPins;
    TextView[] tvwCodes;
    TextView tvwInst;

    Handler handler;
    Runnable rRunNextActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        getSupportActionBar().setTitle(R.string.pin_title);

        btnPins = new Button[10];
        final int[] btnId = new int[] {
                R.id.btn_pin_0, R.id.btn_pin_1, R.id.btn_pin_2,
                R.id.btn_pin_3, R.id.btn_pin_4, R.id.btn_pin_5,
                R.id.btn_pin_6, R.id.btn_pin_7, R.id.btn_pin_8, R.id.btn_pin_9
        };

        for (int i = 0; i < 10; i++) {
            final int ii = i;
            btnPins[i] = findViewById(btnId[i]);
            btnPins[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EnterCode(ii);
                }
            });
        }

        tvwCodes = new TextView[4];
        final int[] tvwId = new int[] {
                R.id.tvw_pin_1, R.id.tvw_pin_2, R.id.tvw_pin_3, R.id.tvw_pin_4
        };

        for (int i = 0; i < 4; i++) {
            tvwCodes[i] = findViewById(tvwId[i]);
        }

        tvwInst = findViewById(R.id.tvw_pin_inst);

        handler = new Handler();

        // initialize preference
        sp = getSharedPreferences("cdp.t9.settings", Activity.MODE_PRIVATE);

        mode = getIntent().getIntExtra("mode", -1);
        switch (mode) {
            case PIN_MODE_INPUT:
                targetIntent = getIntent().getParcelableExtra("activity_to_launch");
                if (targetIntent == null) finish();
                if (sp.getString("pin_code", null) == null) finish();

                sb = new StringBuilder();
                rRunNextActivity = new Runnable() {
                    @Override
                    public void run() {
                        startActivity(targetIntent);
                        finish();
                    }
                };

                tvwInst.setText(R.string.pin_inst_enter);
                break;
            case PIN_MODE_CREATE:
                targetIntent = null;

                sb = new StringBuilder();

                tvwInst.setText(R.string.pin_inst_create);
                break;
            default:
                finish();
        }
    }

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
}
