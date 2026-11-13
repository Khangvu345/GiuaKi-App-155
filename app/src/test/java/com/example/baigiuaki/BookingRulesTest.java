package com.example.baigiuaki;

import org.junit.Test;
import static org.junit.Assert.*;

public class BookingRulesTest {
    @Test public void tinhTienKhongTranSoNguyen() {
        assertEquals(600_000L, BookingRules.total(300_000, 2));
        assertEquals(23_999_999_976L, BookingRules.total(999_999_999, 24));
    }
    @Test public void choPhepHaiLuotLienKe() {
        assertFalse(BookingRules.overlaps(18, 20, 20, 22));
        assertFalse(BookingRules.overlaps(16, 18, 18, 20));
    }
    @Test public void nhanDienCacKieuTrungGio() {
        assertTrue(BookingRules.overlaps(18, 20, 18, 20));
        assertTrue(BookingRules.overlaps(18, 20, 19, 21));
        assertTrue(BookingRules.overlaps(18, 20, 17, 19));
        assertTrue(BookingRules.overlaps(18, 20, 17, 21));
        assertTrue(BookingRules.overlaps(17, 21, 18, 20));
    }
    @Test public void chuanHoaMaVaSoDienThoai() {
        assertEquals("SB004", BookingRules.code(" sb004 "));
        assertEquals("+84912345678", BookingRules.phone("+84 912 345 678"));
        BookingRules.validateBooking("Nguyễn Minh Anh", "0912 345 678", 2000, 2, 1000);
    }
    @Test public void tuChoiThongTinThieuVaSai() {
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validatePitch("", "Sân A", "Hà Nội", 100));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validatePitch("SB1", " ", "Hà Nội", 100));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validatePitch("SB1", "Sân A", " ", 100));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validatePitch("SB1", "Sân A", "Hà Nội", 0));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validateBooking(" ", "0912345678", 2000, 2, 1000));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validateBooking("Anh", "123", 2000, 2, 1000));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.validateBooking("Anh", "0912345678", 1000, 2, 1000));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.total(300000, 0));
        assertThrows(IllegalArgumentException.class, () -> BookingRules.total(-1, 2));
    }
}
