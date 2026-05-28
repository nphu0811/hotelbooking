-- Add thumbnail_url column to hotels table (safe, nullable)
ALTER TABLE hotels ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(1000);

-- Backfill thumbnail_url from first room's primary image for each hotel
UPDATE hotels h SET thumbnail_url = (
    SELECT ri.image_url FROM room_images ri
    JOIN rooms r ON ri.room_id = r.room_id
    WHERE r.hotel_id = h.hotel_id AND r.is_deleted = false AND ri.is_primary = true
    ORDER BY r.created_at ASC LIMIT 1
) WHERE h.thumbnail_url IS NULL;
