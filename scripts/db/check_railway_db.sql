SHOW timezone;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

SELECT indexname
FROM pg_indexes
WHERE schemaname = 'public'
ORDER BY indexname;

SELECT conname, contype
FROM pg_constraint
WHERE connamespace = 'public'::regnamespace
ORDER BY conname;

SELECT
    (SELECT COUNT(*) FROM hotels) AS hotels_count,
    (SELECT COUNT(*) FROM rooms) AS rooms_count,
    (SELECT COUNT(*) FROM bookings) AS bookings_count,
    (SELECT COUNT(*) FROM payments) AS payments_count,
    (SELECT COUNT(*) FROM reviews) AS reviews_count;
