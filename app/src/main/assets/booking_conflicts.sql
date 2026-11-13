SELECT COUNT(*) FROM bookings
WHERE pitch_id = ? AND status = 'BOOKED'
  AND start_ms < ? AND end_ms > ?
