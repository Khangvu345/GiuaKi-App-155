package com.example.baigiuaki;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class AppDatabase extends SQLiteOpenHelper {
    private static AppDatabase instance;
    private final Context context;
    private final boolean seed;
    private final String conflictSql;

    static synchronized AppDatabase get(Context context) {
        if (instance == null) instance = new AppDatabase(context.getApplicationContext(), "san_nha.db", true);
        return instance;
    }

    AppDatabase(Context context, String name, boolean seed) {
        super(context.getApplicationContext(), name, null, 1);
        this.context = context.getApplicationContext();
        this.seed = seed;
        conflictSql = asset("booking_conflicts.sql");
    }

    private String asset(String name) {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Không thể đọc cấu trúc dữ liệu của ứng dụng.", error);
        }
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        for (String sql : asset("schema.sql").split(";")) {
            if (!sql.trim().isEmpty()) db.execSQL(sql);
        }
        if (seed) {
            insertPitch(db, "SB001", "Sân Mini 01", "12 Nguyễn Trãi, Hà Nội", 300_000);
            insertPitch(db, "SB002", "Sân Mini 02", "28 Nguyễn Xiển, Hà Nội", 350_000);
            insertPitch(db, "SB003", "Sân Mini 03", "46 Nguyễn Trãi, Hà Nội", 250_000);
        }
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("Chưa có quy trình nâng cấp dữ liệu cho phiên bản này.");
    }

    private long insertPitch(SQLiteDatabase db, String code, String name, String address, long price) {
        ContentValues values = new ContentValues();
        values.put("code", BookingRules.code(code));
        values.put("name", name.trim());
        values.put("address", address.trim());
        values.put("price", price);
        return db.insertOrThrow("pitches", null, values);
    }

    long addPitch(String code, String name, String address, long price) {
        BookingRules.validatePitch(code, name, address, price);
        try {
            return insertPitch(getWritableDatabase(), code, name, address, price);
        } catch (SQLiteConstraintException error) {
            throw new IllegalArgumentException("Mã sân đã tồn tại. Hãy chọn mã khác.");
        }
    }

    private Models.Pitch readPitch(Cursor c) {
        Models.Pitch p = new Models.Pitch();
        p.id = c.getLong(c.getColumnIndexOrThrow("id"));
        p.code = c.getString(c.getColumnIndexOrThrow("code"));
        p.name = c.getString(c.getColumnIndexOrThrow("name"));
        p.address = c.getString(c.getColumnIndexOrThrow("address"));
        p.price = c.getLong(c.getColumnIndexOrThrow("price"));
        return p;
    }

    List<Models.Pitch> pitches(long start, int hours) {
        List<Models.Pitch> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT * FROM pitches WHERE is_deleted = 0 ORDER BY code COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                Models.Pitch p = readPitch(c);
                p.available = !hasConflict(db, p.id, start, start + hours * BookingRules.HOUR_MS);
                list.add(p);
            }
        }
        return list;
    }

    private Models.Pitch pitch(SQLiteDatabase db, long id) {
        try (Cursor c = db.rawQuery("SELECT * FROM pitches WHERE id = ? AND is_deleted = 0", new String[]{Long.toString(id)})) {
            if (!c.moveToFirst()) throw new IllegalArgumentException("Sân bóng không còn trong danh sách.");
            return readPitch(c);
        }
    }

    private boolean hasConflict(SQLiteDatabase db, long pitchId, long start, long end) {
        try (Cursor c = db.rawQuery(conflictSql, new String[]{Long.toString(pitchId), Long.toString(end), Long.toString(start)})) {
            c.moveToFirst();
            return c.getLong(0) > 0;
        }
    }

    boolean available(long pitchId, long start, int hours) {
        SQLiteDatabase db = getReadableDatabase();
        pitch(db, pitchId);
        return !hasConflict(db, pitchId, start, start + hours * BookingRules.HOUR_MS);
    }

    long createBooking(long pitchId, String customer, String phone, long start, int hours) {
        BookingRules.validateBooking(customer, phone, start, hours, System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            Models.Pitch p = pitch(db, pitchId);
            long end = start + hours * BookingRules.HOUR_MS;
            // Kiểm tra và lưu trong cùng giao dịch để tránh hai đơn trùng giờ.
            if (hasConflict(db, pitchId, start, end))
                throw new IllegalArgumentException("Sân đã có lịch đặt trùng thời gian. Vui lòng chọn khung giờ khác.");
            ContentValues values = new ContentValues();
            values.put("pitch_id", p.id);
            values.put("pitch_code", p.code);
            values.put("pitch_name", p.name);
            values.put("address", p.address);
            values.put("customer", customer.trim());
            values.put("phone", BookingRules.phone(phone));
            values.put("start_ms", start);
            values.put("end_ms", end);
            values.put("hours", hours);
            values.put("unit_price", p.price);
            values.put("total", BookingRules.total(p.price, hours));
            values.put("status", "BOOKED");
            values.put("created_at", System.currentTimeMillis());
            long id = db.insertOrThrow("bookings", null, values);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    void deletePitch(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            pitch(db, id);
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE pitch_id = ? AND status = 'BOOKED' AND end_ms > ?",
                    new String[]{Long.toString(id), Long.toString(System.currentTimeMillis())})) {
                c.moveToFirst();
                if (c.getInt(0) > 0) throw new IllegalArgumentException("Sân còn lịch đang diễn ra hoặc sắp tới. Hãy hủy các đơn đó trước khi xóa sân.");
            }
            // Ẩn sân khỏi danh sách nhưng giữ thông tin trong lịch sử đơn.
            ContentValues values = new ContentValues();
            values.put("is_deleted", 1);
            db.update("pitches", values, "id = ?", new String[]{Long.toString(id)});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void cancel(long id) {
        ContentValues values = new ContentValues();
        values.put("status", "CANCELLED");
        int changed = getWritableDatabase().update("bookings", values, "id = ? AND status = 'BOOKED'", new String[]{Long.toString(id)});
        if (changed == 0) throw new IllegalArgumentException("Đơn không tồn tại hoặc đã được hủy.");
    }

    private Models.Booking readBooking(Cursor c) {
        Models.Booking b = new Models.Booking();
        b.id = c.getLong(c.getColumnIndexOrThrow("id"));
        b.pitchId = c.getLong(c.getColumnIndexOrThrow("pitch_id"));
        b.pitchCode = c.getString(c.getColumnIndexOrThrow("pitch_code"));
        b.pitchName = c.getString(c.getColumnIndexOrThrow("pitch_name"));
        b.address = c.getString(c.getColumnIndexOrThrow("address"));
        b.customer = c.getString(c.getColumnIndexOrThrow("customer"));
        b.phone = c.getString(c.getColumnIndexOrThrow("phone"));
        b.start = c.getLong(c.getColumnIndexOrThrow("start_ms"));
        b.end = c.getLong(c.getColumnIndexOrThrow("end_ms"));
        b.hours = c.getInt(c.getColumnIndexOrThrow("hours"));
        b.price = c.getLong(c.getColumnIndexOrThrow("unit_price"));
        b.total = c.getLong(c.getColumnIndexOrThrow("total"));
        b.status = c.getString(c.getColumnIndexOrThrow("status"));
        return b;
    }

    List<Models.Booking> bookings(String status) {
        List<Models.Booking> result = new ArrayList<>();
        String sql = "SELECT * FROM bookings" + (status == null ? "" : " WHERE status = ?") + " ORDER BY id DESC";
        try (Cursor c = getReadableDatabase().rawQuery(sql, status == null ? null : new String[]{status})) {
            while (c.moveToNext()) result.add(readBooking(c));
        }
        return result;
    }

    Models.Booking booking(long id) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM bookings WHERE id = ?", new String[]{Long.toString(id)})) {
            if (!c.moveToFirst()) throw new IllegalArgumentException("Không tìm thấy đơn đặt sân.");
            return readBooking(c);
        }
    }
}
