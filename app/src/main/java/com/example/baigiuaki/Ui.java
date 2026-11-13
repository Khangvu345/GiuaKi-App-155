package com.example.baigiuaki;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.graphics.drawable.Drawable;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class Ui {
    static final Locale VI = new Locale("vi", "VN");
    static final TimeZone ZONE = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    interface TimeCallback { void accept(long time); }
    interface HoursCallback { void accept(int hours); }
    private Ui() {}

    static String money(long value) { return NumberFormat.getIntegerInstance(VI).format(value) + " đ"; }
    static String code(long id) { return String.format(Locale.ROOT, "#DS%03d", id); }
    static String format(long time, String pattern) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, VI);
        f.setTimeZone(ZONE);
        return f.format(new Date(time));
    }
    static String date(long time) { return format(time, "dd/MM/yyyy"); }
    static String time(long time) { return format(time, "HH:mm"); }
    static String range(long start, long end) {
        return time(start) + " – " + time(end) + (date(start).equals(date(end)) ? "" : " (" + date(end) + ")");
    }
    static String ending(long start, long end) {
        return time(end) + (date(start).equals(date(end)) ? "" : " · " + date(end));
    }
    static Calendar calendar(long value) {
        Calendar c = Calendar.getInstance(ZONE, VI);
        c.setTimeInMillis(value);
        return c;
    }
    static long nextHour() {
        Calendar c = calendar(System.currentTimeMillis());
        c.add(Calendar.HOUR_OF_DAY, 1);
        c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    static void chooseDate(Activity a, long value, boolean green, TimeCallback callback) {
        Calendar selected = calendar(value);
        DatePickerDialog dialog = new DatePickerDialog(a, green ? R.style.Theme_SanNha_GreenDialog : 0,
                (view, y, m, d) -> {
                    selected.set(y, m, d);
                    callback.accept(selected.getTimeInMillis());
                }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH));
        Calendar today = calendar(System.currentTimeMillis());
        today.set(Calendar.HOUR_OF_DAY, 0); today.set(Calendar.MINUTE, 0); today.set(Calendar.SECOND, 0); today.set(Calendar.MILLISECOND, 0);
        dialog.getDatePicker().setMinDate(today.getTimeInMillis());
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Chọn", dialog);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Đóng", (d, w) -> d.dismiss());
        dialog.show();
    }

    static void chooseTime(Activity a, long value, boolean green, TimeCallback callback) {
        Calendar c = calendar(value);
        TimePickerDialog dialog = new TimePickerDialog(a, green ? R.style.Theme_SanNha_GreenDialog : 0,
                (view, hour, minute) -> {
                    c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, minute);
                    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
                    callback.accept(c.getTimeInMillis());
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true);
        dialog.setTitle("Chọn giờ bắt đầu");
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Chọn", dialog);
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Đóng", (d, w) -> d.dismiss());
        dialog.show();
    }

    static void chooseHours(Activity a, int value, HoursCallback callback) {
        NumberPicker picker = new NumberPicker(a);
        picker.setMinValue(1); picker.setMaxValue(24); picker.setValue(value);
        picker.setWrapSelectorWheel(false);
        new AlertDialog.Builder(a).setTitle("Chọn số giờ thuê").setView(picker)
                .setNegativeButton("Đóng", null).setPositiveButton("Chọn", (d, w) -> {
                    picker.clearFocus(); callback.accept(picker.getValue());
                }).show();
    }

    static void status(TextView text, boolean active, boolean pitch) {
        text.setText(active ? (pitch ? "Còn trống" : "Đã đặt") : (pitch ? "Đã được đặt" : "Đã hủy"));
        text.setBackgroundResource(active ? R.drawable.bg_green_soft : (pitch ? R.drawable.bg_amber : R.drawable.bg_gray));
        text.setTextColor(text.getContext().getColor(active ? R.color.green_dark : (pitch ? R.color.amber_text : R.color.muted)));
    }

    static void nav(Activity a, boolean pitches) {
        TextView left = a.findViewById(R.id.navPitches), right = a.findViewById(R.id.navBookings);
        navStyle(left, pitches); navStyle(right, !pitches);
        left.setOnClickListener(v -> {
            if (!pitches) {
                a.startActivity(new Intent(a, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                a.finish();
            }
        });
        right.setOnClickListener(v -> { if (pitches) a.startActivity(new Intent(a, BookingsActivity.class)); });
    }

    private static void navStyle(TextView v, boolean active) {
        int color = v.getContext().getColor(active ? R.color.primary : R.color.muted);
        v.setTextColor(color);
        v.setSelected(active);
        v.setBackgroundResource(active ? R.drawable.bg_red_soft : android.R.color.transparent);
        for (Drawable d : v.getCompoundDrawablesRelative()) if (d != null) d.mutate().setTint(color);
    }

    static int dp(Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
}
