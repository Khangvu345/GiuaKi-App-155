package com.example.baigiuaki;

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

public class BookingsActivity extends BaseActivity {
    private String filter;
    private int request;
    private final List<Models.Booking> items = new ArrayList<>();
    private final BookingAdapter adapter = new BookingAdapter();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); screen(R.layout.activity_bookings);
        filter = state == null ? null : state.getString("filter");
        ListView list = findViewById(R.id.listBookings);
        list.setAdapter(adapter); list.setEmptyView(findViewById(R.id.emptyBookings));
        findViewById(R.id.tabAll).setOnClickListener(v -> { filter = null; refresh(); });
        findViewById(R.id.tabBooked).setOnClickListener(v -> { filter = "BOOKED"; refresh(); });
        findViewById(R.id.tabCancelled).setOnClickListener(v -> { filter = "CANCELLED"; refresh(); });
        Ui.nav(this, false);
    }

    @Override protected void onResume() { super.onResume(); refresh(); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("filter", filter); }

    private void tab(int id, boolean active) {
        Button b = findViewById(id); b.setSelected(active);
        b.setBackgroundResource(active ? R.drawable.bg_button_red : R.drawable.bg_input);
        b.setTextColor(getColor(active ? R.color.white : R.color.text));
    }

    private void refresh() {
        tab(R.id.tabAll, filter == null); tab(R.id.tabBooked, "BOOKED".equals(filter)); tab(R.id.tabCancelled, "CANCELLED".equals(filter));
        int ticket = ++request; String status = filter;
        task(() -> db.bookings(status), list -> {
            if (ticket != request) return;
            items.clear(); items.addAll(list); adapter.notifyDataSetChanged();
            ((TextView) findViewById(R.id.tvBookingCount)).setText(items.size() + " đơn đặt");
        });
    }

    private void details(Models.Booking b) {
        startActivity(new Intent(this, BookingDetailActivity.class).putExtra("booking_id", b.id));
    }

    private final class BookingAdapter extends BaseAdapter {
        @Override public int getCount() { return items.size(); }
        @Override public Models.Booking getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return items.get(position).id; }
        @Override public View getView(int position, View view, ViewGroup parent) {
            if (view == null) view = LayoutInflater.from(BookingsActivity.this).inflate(R.layout.item_booking, parent, false);
            Models.Booking b = getItem(position);
            ((TextView) view.findViewById(R.id.tvBookingCode)).setText(Ui.code(b.id));
            Ui.status(view.findViewById(R.id.tvBookingStatus), b.isBooked(), false);
            ((TextView) view.findViewById(R.id.tvCustomer)).setText(b.customer);
            ((TextView) view.findViewById(R.id.tvBookedPitch)).setText(b.pitchName);
            ((TextView) view.findViewById(R.id.tvBookedDate)).setText(Ui.date(b.start) + " · " + Ui.time(b.start));
            ((TextView) view.findViewById(R.id.tvBookedHours)).setText(b.hours + " giờ · Kết thúc " + Ui.ending(b.start, b.end));
            ((TextView) view.findViewById(R.id.tvBookedTotal)).setText(Ui.money(b.total));
            View cancel = view.findViewById(R.id.btnCancel);
            cancel.setVisibility(b.isBooked() ? View.VISIBLE : View.GONE);
            cancel.setOnClickListener(v -> confirmCancel(b, BookingsActivity.this::refresh));
            view.findViewById(R.id.btnDetails).setOnClickListener(v -> details(b));
            return view;
        }
    }
}
