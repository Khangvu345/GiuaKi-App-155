CREATE TABLE pitches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL COLLATE NOCASE UNIQUE,
    name TEXT NOT NULL CHECK(length(trim(name)) > 0),
    address TEXT NOT NULL CHECK(length(trim(address)) > 0),
    price INTEGER NOT NULL CHECK(price BETWEEN 1 AND 999999999),
    is_deleted INTEGER NOT NULL DEFAULT 0 CHECK(is_deleted IN (0, 1))
);
CREATE TABLE bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    pitch_id INTEGER NOT NULL REFERENCES pitches(id),
    pitch_code TEXT NOT NULL,
    pitch_name TEXT NOT NULL,
    address TEXT NOT NULL,
    customer TEXT NOT NULL CHECK(length(trim(customer)) > 0),
    phone TEXT NOT NULL,
    start_ms INTEGER NOT NULL,
    end_ms INTEGER NOT NULL,
    hours INTEGER NOT NULL CHECK(hours BETWEEN 1 AND 24),
    unit_price INTEGER NOT NULL CHECK(unit_price > 0),
    total INTEGER NOT NULL CHECK(total = unit_price * hours),
    status TEXT NOT NULL DEFAULT 'BOOKED' CHECK(status IN ('BOOKED', 'CANCELLED')),
    created_at INTEGER NOT NULL,
    CHECK(end_ms = start_ms + hours * 3600000)
);
CREATE INDEX idx_booking_schedule ON bookings(pitch_id, status, start_ms, end_ms);
