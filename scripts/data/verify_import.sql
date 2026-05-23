SELECT COUNT(*) AS hotels_count FROM hotels;
SELECT COUNT(*) AS rooms_count FROM rooms;
SELECT COUNT(*) AS amenities_count FROM amenities;
SELECT COUNT(*) AS room_images_count FROM room_images;
SELECT hotel_id, name, city, province FROM hotels ORDER BY name LIMIT 20;
