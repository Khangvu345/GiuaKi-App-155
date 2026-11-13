package com.example.baigiuaki;

import java.util.Locale;

public final class BookingRules {
    public static final long HOUR_MS = 3_600_000L;
    private BookingRules() {}

    public static String code(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public static String phone(String value) {
        return value.replaceAll("[\\s().-]", "");
    }

    public static void validatePitch(String code, String name, String address, long price) {
        if (!code(code).matches("[A-Z0-9_-]{1,20}"))
            throw new IllegalArgumentException("Mã sân gồm 1–20 chữ cái, chữ số, dấu gạch ngang hoặc gạch dưới.");
        if (name.trim().isEmpty() || name.trim().length() > 100)
            throw new IllegalArgumentException("Tên sân phải có từ 1 đến 100 ký tự.");
        if (address.trim().isEmpty() || address.trim().length() > 250)
            throw new IllegalArgumentException("Địa chỉ phải có từ 1 đến 250 ký tự.");
        if (price <= 0 || price > 999_999_999L)
            throw new IllegalArgumentException("Giá thuê phải từ 1 đến 999.999.999 đồng mỗi giờ.");
    }

    public static void validateBooking(String name, String phone, long start, int hours, long now) {
        if (name.trim().isEmpty() || name.trim().length() > 100)
            throw new IllegalArgumentException("Họ tên phải có từ 1 đến 100 ký tự.");
        if (!phone(phone).matches("\\+?[0-9]{9,15}"))
            throw new IllegalArgumentException("Số điện thoại phải gồm 9–15 chữ số, có thể bắt đầu bằng dấu +.");
        if (hours < 1 || hours > 24)
            throw new IllegalArgumentException("Số giờ thuê phải từ 1 đến 24 giờ.");
        if (start <= now)
            throw new IllegalArgumentException("Giờ bắt đầu phải sau thời điểm hiện tại.");
    }

    public static long total(long unitPrice, int hours) {
        if (unitPrice <= 0 || unitPrice > 999_999_999L || hours < 1 || hours > 24)
            throw new IllegalArgumentException("Đơn giá hoặc số giờ thuê không hợp lệ.");
        return Math.multiplyExact(unitPrice, (long) hours);
    }

    public static boolean overlaps(long aStart, long aEnd, long bStart, long bEnd) {
        return aStart < bEnd && aEnd > bStart;
    }
}
