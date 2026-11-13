package com.example.baigiuaki;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    private Context context;
    private AppDatabase db;
    private final String databaseName = "kiem_thu_san_nha.db";
    private long pitch, start;

    @Before public void chuanBi() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(databaseName);
        db = new AppDatabase(context, databaseName, false);
        pitch = db.addPitch("SB001", "Sân thử nghiệm", "Hà Nội", 300000);
        start = Ui.nextHour() + 7 * 24 * BookingRules.HOUR_MS;
    }
    @After public void donDep() { db.close(); context.deleteDatabase(databaseName); }

    @Test public void luuDuThongTinVaMoLai() {
        long id = db.createBooking(pitch, "Nguyễn Minh Anh", "0912 345 678", start, 2);
        db.close(); db = new AppDatabase(context, databaseName, false);
        Models.Booking b = db.booking(id);
        assertEquals("Nguyễn Minh Anh", b.customer); assertEquals("0912345678", b.phone);
        assertEquals("Sân thử nghiệm", b.pitchName); assertEquals("SB001", b.pitchCode);
        assertEquals("Hà Nội", b.address); assertEquals(2, b.hours);
        assertEquals(300000, b.price); assertEquals(600000, b.total);
        assertEquals(start, b.start); assertEquals(start + 2 * BookingRules.HOUR_MS, b.end);
        assertTrue(b.isBooked()); assertEquals(1, db.pitches(start, 2).size());
    }
    @Test public void chanTrungGioVaChoPhepLienKe() {
        db.createBooking(pitch, "Anh", "0912345678", start, 2);
        long h = BookingRules.HOUR_MS;
        long[] times = {start, start - h, start + h, start - h};
        int[] durations = {2, 2, 2, 4};
        for (int i = 0; i < times.length; i++) {
            final long time = times[i]; final int duration = durations[i];
            assertThrows(IllegalArgumentException.class, () -> db.createBooking(pitch, "Bình", "0987654321", time, duration));
        }
        db.createBooking(pitch, "Bình", "0987654321", start + 2 * h, 1);
        db.createBooking(pitch, "Bình", "0987654321", start - h, 1);
        assertEquals(3, db.bookings(null).size());
    }
    @Test public void haiSanDuocDatCungGio() {
        long other = db.addPitch("SB002", "Sân khác", "Hà Nội", 350000);
        db.createBooking(pitch, "Anh", "0912345678", start, 2);
        db.createBooking(other, "Bình", "0987654321", start, 2);
        assertEquals(2, db.bookings("BOOKED").size());
    }
    @Test public void huyGiaiPhongKhungGioNhungGiuLichSu() {
        long id = db.createBooking(pitch, "Anh", "0912345678", start, 2);
        assertFalse(db.available(pitch, start, 2));
        db.cancel(id);
        assertTrue(db.available(pitch, start, 2));
        assertFalse(db.booking(id).isBooked());
        db.createBooking(pitch, "Bình", "0987654321", start, 2);
        assertEquals(2, db.bookings(null).size()); assertEquals(1, db.bookings("CANCELLED").size());
    }
    @Test public void xoaSanKhongLamMatDonCu() {
        long id = db.createBooking(pitch, "Anh", "0912345678", start, 2);
        assertThrows(IllegalArgumentException.class, () -> db.deletePitch(pitch));
        db.cancel(id); db.deletePitch(pitch);
        assertTrue(db.pitches(start, 2).isEmpty());
        assertEquals("Sân thử nghiệm", db.booking(id).pitchName);
        assertThrows(IllegalArgumentException.class, () -> db.createBooking(pitch, "Anh", "0912345678", start, 2));
    }
    @Test public void maSanKhongPhanBietHoaThuong() {
        assertThrows(IllegalArgumentException.class, () -> db.addPitch("sb001", "Sân khác", "Hà Nội", 100000));
    }
    @Test public void datQuaNuaDemVanChanTrungGio() {
        Calendar c = Ui.calendar(start); c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 0);
        long at23 = c.getTimeInMillis();
        db.createBooking(pitch, "Anh", "0912345678", at23, 2);
        assertFalse(db.available(pitch, at23 + BookingRules.HOUR_MS, 1));
        assertTrue(db.available(pitch, at23 + 2 * BookingRules.HOUR_MS, 1));
        assertTrue(Ui.range(at23, at23 + 2 * BookingRules.HOUR_MS).contains(Ui.date(at23 + 2 * BookingRules.HOUR_MS)));
    }
    @Test public void haiYeuCauDongThoiChiTaoMotDon() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Boolean> work = () -> {
                ready.await();
                try { db.createBooking(pitch, "Anh", "0912345678", start, 2); return true; }
                catch (IllegalArgumentException error) { return false; }
            };
            Future<Boolean> a = pool.submit(work), b = pool.submit(work); ready.countDown();
            assertNotEquals(a.get(), b.get()); assertEquals(1, db.bookings(null).size());
        } finally { pool.shutdownNow(); }
    }
}
