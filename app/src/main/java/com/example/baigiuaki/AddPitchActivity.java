package com.example.baigiuaki;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class AddPitchActivity extends BaseActivity {
    private EditText code, name, address, price;
    private Button save;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_add_pitch);
        code = findViewById(R.id.etCode); name = findViewById(R.id.etPitchName);
        address = findViewById(R.id.etAddress); price = findViewById(R.id.etPrice);
        save = findViewById(R.id.btnSavePitch);
        save.setOnClickListener(v -> save());
    }

    private boolean required(EditText input, String message) {
        if (input.getText().toString().trim().isEmpty()) {
            input.setError(message); input.requestFocus(); return false;
        }
        return true;
    }

    private void save() {
        if (!required(code, "Vui lòng nhập mã sân.") || !required(name, "Vui lòng nhập tên sân.")
                || !required(address, "Vui lòng nhập địa chỉ.") || !required(price, "Vui lòng nhập giá thuê.")) return;
        long amount;
        try { amount = Long.parseLong(price.getText().toString().trim()); }
        catch (NumberFormatException error) { price.setError("Giá thuê phải là số nguyên hợp lệ."); return; }
        String c = code.getText().toString(), n = name.getText().toString(), a = address.getText().toString();
        try { BookingRules.validatePitch(c, n, a, amount); }
        catch (IllegalArgumentException error) { showError(error); return; }
        save.setEnabled(false); save.setText("Đang lưu…");
        task(() -> db.addPitch(c, n, a, amount), id -> {
            toast("Đã thêm sân bóng."); finish();
        }, error -> {
            save.setEnabled(true); save.setText(R.string.save_pitch); showError(error);
        });
    }
}
