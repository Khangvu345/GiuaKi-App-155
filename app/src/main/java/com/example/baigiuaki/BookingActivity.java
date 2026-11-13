package com.example.baigiuaki;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class BookingActivity extends BaseActivity {
    private long start, selectedId;
    private int hours, request;
    private boolean saving;
    private Models.Pitch selected;
    private final List<Models.Pitch> pitches = new ArrayList<>();
    private EditText customer, phone;
    private Button confirm;
    private TextView availability;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_booking);
        start = state == null ? getIntent().getLongExtra("start", Ui.nextHour()) : state.getLong("start");
        hours = state == null ? getIntent().getIntExtra("hours", 2) : state.getInt("hours", 2);
        selectedId = state == null ? getIntent().getLongExtra("pitch_id", -1) : state.getLong("pitch_id", -1);
        customer = findViewById(R.id.etCustomer); phone = findViewById(R.id.etPhone);
        confirm = findViewById(R.id.btnConfirm); availability = findViewById(R.id.tvAvailability);
        confirm.setEnabled(false);
        findViewById(R.id.btnSelectPitch).setOnClickListener(v -> selectPitch());
        findViewById(R.id.btnDate).setOnClickListener(v -> Ui.chooseDate(this, start, true, t -> { start = t; update(); }));
        findViewById(R.id.btnTime).setOnClickListener(v -> Ui.chooseTime(this, start, true, t -> { start = t; update(); }));
        findViewById(R.id.btnHours).setOnClickListener(v -> Ui.chooseHours(this, hours, h -> { hours = h; update(); }));
        confirm.setOnClickListener(v -> save());
    }

    @Override protected void onResume() { super.onResume(); loadPitches(); }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putLong("start", start); out.putInt("hours", hours); out.putLong("pitch_id", selectedId);
    }

    private void loadPitches() {
        long from = start; int duration = hours;
        task(() -> db.pitches(from, duration), list -> {
            pitches.clear(); pitches.addAll(list);
            selected = null;
            for (Models.Pitch p : list) if (p.id == selectedId) selected = p;
            if (selected == null && !list.isEmpty()) selected = list.get(0);
            selectedId = selected == null ? -1 : selected.id;
            update();
        });
    }

    private void selectPitch() {
        if (saving) return;
        if (pitches.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Chưa có sân bóng")
                    .setMessage("Bạn cần thêm ít nhất một sân trước khi đặt.")
                    .setNegativeButton("Đóng", null).setPositiveButton("Thêm sân", (d, w) ->
                            startActivity(new Intent(this, AddPitchActivity.class))).show();
            return;
        }
        String[] labels = new String[pitches.size()];
        int checked = 0;
        for (int i = 0; i < pitches.size(); i++) {
            Models.Pitch p = pitches.get(i);
            labels[i] = p.label() + " — " + Ui.money(p.price) + " / giờ";
            if (p.id == selectedId) checked = i;
        }
        new AlertDialog.Builder(this, R.style.Theme_SanNha_GreenDialog).setTitle("Chọn sân bóng")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    selected = pitches.get(which); selectedId = selected.id; d.dismiss(); update();
                }).setNegativeButton("Đóng", null).show();
    }

    private void update() {
        ((TextView) findViewById(R.id.btnDate)).setText(Ui.date(start));
        ((TextView) findViewById(R.id.btnTime)).setText(Ui.time(start));
        ((TextView) findViewById(R.id.btnHours)).setText(hours + " giờ");
        ((TextView) findViewById(R.id.tvSelectedPitch)).setText(selected == null ? "Chọn sân bóng" : selected.label());
        ((TextView) findViewById(R.id.tvSelectedPrice)).setText(selected == null ? "Chưa có sân trong danh sách" : Ui.money(selected.price) + " / giờ");
        long total = selected == null ? 0 : BookingRules.total(selected.price, hours);
        ((TextView) findViewById(R.id.tvTotal)).setText(Ui.money(total));
        ((TextView) findViewById(R.id.tvCalculation)).setText(selected == null ? "Chọn sân để tính tổng tiền" : Ui.money(selected.price) + " × " + hours + " giờ");
        int ticket = ++request;
        confirm.setEnabled(false);
        if (selected == null) { setAvailability("Chưa có sân bóng để đặt.", false); return; }
        if (start <= System.currentTimeMillis()) { setAvailability("Vui lòng chọn giờ bắt đầu trong tương lai.", false); return; }
        setAvailability("Đang kiểm tra khung giờ…", true);
        long pitchId = selected.id, from = start; int duration = hours;
        task(() -> db.available(pitchId, from, duration), free -> {
            if (ticket != request) return;
            setAvailability(free ? "Khung giờ còn trống" : "Trùng lịch đặt. Vui lòng chọn giờ khác.", free);
            confirm.setEnabled(free && !saving);
        }, error -> { if (ticket == request) { setAvailability("Không thể kiểm tra lịch. Vui lòng thử lại.", false); showError(error); } });
    }

    private void setAvailability(String text, boolean free) {
        availability.setText(text);
        availability.setTextColor(getColor(free ? R.color.green_dark : R.color.primary));
        availability.setBackgroundResource(free ? R.drawable.bg_green_soft : R.drawable.bg_red_soft);
    }

    private void save() {
        if (saving || selected == null) return;
        String name = customer.getText().toString().trim(), tel = phone.getText().toString().trim();
        if (name.isEmpty()) { customer.setError("Vui lòng nhập họ tên."); customer.requestFocus(); return; }
        if (tel.isEmpty()) { phone.setError("Vui lòng nhập số điện thoại."); phone.requestFocus(); return; }
        try { BookingRules.validateBooking(name, tel, start, hours, System.currentTimeMillis()); }
        catch (IllegalArgumentException error) { showError(error); return; }
        saving = true; confirm.setEnabled(false); confirm.setText("Đang lưu…");
        long pitchId = selected.id, from = start; int duration = hours;
        task(() -> db.createBooking(pitchId, name, tel, from, duration), id -> {
            toast("Đặt sân thành công.");
            startActivity(new Intent(this, BookingDetailActivity.class).putExtra("booking_id", id));
            finish();
        }, error -> {
            saving = false; confirm.setText(R.string.confirm_booking); update(); showError(error);
        });
    }
}
