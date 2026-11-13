package com.example.baigiuaki;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {
    private long start;
    private int hours, request;
    private View filterHeader;
    private final List<Models.Pitch> items = new ArrayList<>();
    private final PitchAdapter adapter = new PitchAdapter();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        screen(R.layout.activity_main);
        start = state == null ? Ui.nextHour() : state.getLong("start");
        hours = state == null ? 2 : state.getInt("hours", 2);
        ListView list = findViewById(R.id.listPitches);
        filterHeader = getLayoutInflater().inflate(R.layout.header_pitch_list, list, false);
        list.addHeaderView(filterHeader, null, false);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.emptyPitches));
        findViewById(R.id.btnAdd).setOnClickListener(v -> startActivity(new Intent(this, AddPitchActivity.class)));
        filterHeader.findViewById(R.id.filterDate).setOnClickListener(v -> Ui.chooseDate(this, start, false, t -> { start = t; refresh(); }));
        filterHeader.findViewById(R.id.filterTime).setOnClickListener(v -> Ui.chooseTime(this, start, false, t -> { start = t; refresh(); }));
        filterHeader.findViewById(R.id.filterHours).setOnClickListener(v -> Ui.chooseHours(this, hours, h -> { hours = h; refresh(); }));
        Ui.nav(this, true);
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putLong("start", start); out.putInt("hours", hours);
    }

    private void refresh() {
        ((TextView) filterHeader.findViewById(R.id.filterDate)).setText(Ui.date(start));
        ((TextView) filterHeader.findViewById(R.id.filterTime)).setText(Ui.time(start));
        ((TextView) filterHeader.findViewById(R.id.filterHours)).setText(hours + " giờ");
        int ticket = ++request;
        long selectedStart = start;
        int selectedHours = hours;
        task(() -> db.pitches(selectedStart, selectedHours), list -> {
            if (ticket != request) return;
            items.clear(); items.addAll(list); adapter.notifyDataSetChanged();
            ((TextView) filterHeader.findViewById(R.id.tvPitchCount)).setText(items.size() + " sân");
        });
    }

    private void book(Models.Pitch p) {
        startActivity(new Intent(this, BookingActivity.class).putExtra("pitch_id", p.id)
                .putExtra("start", start).putExtra("hours", hours));
    }

    private void delete(Models.Pitch p) {
        new AlertDialog.Builder(this).setTitle("Xóa sân bóng?")
                .setMessage("Xóa " + p.name + " khỏi danh sách? Các đơn trước đây vẫn được giữ trong lịch sử.")
                .setNegativeButton("Giữ sân", null).setPositiveButton("Xóa sân", (d, w) -> task(() -> {
                    db.deletePitch(p.id); return true;
                }, ok -> { toast("Đã xóa sân khỏi danh sách."); refresh(); })).show();
    }

    private final class PitchAdapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Models.Pitch getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return items.get(position).id; }
        @Override public View getView(int position, View view, ViewGroup parent) {
            if (view == null) view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_pitch, parent, false);
            Models.Pitch p = getItem(position);
            ((TextView) view.findViewById(R.id.tvPitchCode)).setText(p.code);
            ((TextView) view.findViewById(R.id.tvPitchName)).setText(p.name);
            ((TextView) view.findViewById(R.id.tvPitchAddress)).setText(p.address);
            ((TextView) view.findViewById(R.id.tvPitchPrice)).setText(Ui.money(p.price) + " / giờ");
            Ui.status(view.findViewById(R.id.tvPitchStatus), p.available, true);
            Button action = view.findViewById(R.id.btnBookPitch);
            action.setText(p.available ? "Đặt sân" : "Chọn giờ khác");
            action.setBackgroundResource(p.available ? R.drawable.bg_button_red : R.drawable.bg_button_outline);
            action.setTextColor(getColor(p.available ? R.color.white : R.color.primary));
            action.setOnClickListener(v -> book(p));
            view.findViewById(R.id.btnDeletePitch).setOnClickListener(v -> delete(p));
            return view;
        }
    }
}
