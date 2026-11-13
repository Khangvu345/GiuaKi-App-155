package com.example.baigiuaki;

final class Models {
    private Models() {}

    static final class Pitch {
        long id;
        String code, name, address;
        long price;
        boolean available;

        String label() { return code + " · " + name; }
    }

    static final class Booking {
        long id, pitchId, start, end, price, total;
        String pitchCode, pitchName, address, customer, phone, status;
        int hours;

        boolean isBooked() { return "BOOKED".equals(status); }
    }
}
