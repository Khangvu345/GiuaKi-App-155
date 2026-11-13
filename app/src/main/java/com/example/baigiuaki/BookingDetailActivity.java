package com.example.baigiuaki;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class BookingDetailActivity extends BaseActivity {
    private long id;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_booking_detail);
        id = getIntent().getLongExtra("booking_id", -1);
        findViewById(R.id.btnCancelDetail).setEnabled(false);
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }

    private void refresh() {
        task(() -> db.booking(id), b -> {
            text(R.id.tvDetailCode, Ui.code(b.id));
            Ui.status(findViewById(R.id.tvDetailStatus), b.isBooked(), false);
            text(R.id.tvDetailCustomer, b.customer); text(R.id.tvDetailPhone, b.phone);
            text(R.id.tvDetailPitch, b.pitchCode + " · " + b.pitchName); text(R.id.tvDetailAddress, b.address);
            text(R.id.tvDetailDate, Ui.date(b.start)); text(R.id.tvDetailTime, Ui.range(b.start, b.end));
            text(R.id.tvDetailHours, b.hours + " giờ"); text(R.id.tvDetailPrice, Ui.money(b.price) + " / giờ");
            text(R.id.tvDetailTotal, Ui.money(b.total));
            text(R.id.tvCancelNote, b.isBooked() ? "Hủy đơn sẽ giải phóng khung giờ đã đặt." : "Đơn đã được hủy. Khung giờ này không còn bị đơn này giữ chỗ.");
            View cancel = findViewById(R.id.btnCancelDetail);
            cancel.setVisibility(b.isBooked() ? View.VISIBLE : View.GONE); cancel.setEnabled(true);
            cancel.setOnClickListener(v -> confirmCancel(b, this::refresh));
        });
    }
}
