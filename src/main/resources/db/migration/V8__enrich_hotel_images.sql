-- V8: Enrich room images with high-quality real-world Unsplash hotel photos based on the hotel's city location
UPDATE room_images ri
SET image_url = CASE 
    -- Hà Nội
    WHEN h.city ILIKE '%Hà Nội%' OR h.city ILIKE '%Hanoi%' THEN 
        CASE 
            WHEN ri.image_url LIKE '%suite%' OR ri.image_url LIKE '%family%' THEN 'https://images.unsplash.com/photo-1582719508461-905c673771fd?auto=format&fit=crop&w=1200&q=80'
            ELSE 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80'
        END
    -- Đà Nẵng
    WHEN h.city ILIKE '%Đà Nẵng%' OR h.city ILIKE '%Danang%' THEN 
        CASE 
            WHEN ri.image_url LIKE '%twin%' OR ri.image_url LIKE '%comfort%' THEN 'https://images.unsplash.com/photo-1590490360182-c33d57733427?auto=format&fit=crop&w=1200&q=80'
            ELSE 'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=1200&q=80'
        END
    -- Đà Lạt
    WHEN h.city ILIKE '%Đà Lạt%' OR h.city ILIKE '%Dalat%' THEN 
        CASE 
            WHEN ri.image_url LIKE '%attic%' OR ri.image_url LIKE '%garden%' THEN 'https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1200&q=80'
            ELSE 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=80'
        END
    -- Phú Quốc
    WHEN h.city ILIKE '%Phú Quốc%' OR h.city ILIKE '%Phu Quoc%' THEN 
        'https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=1200&q=80'
    -- Nha Trang
    WHEN h.city ILIKE '%Nha Trang%' THEN 
        'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?auto=format&fit=crop&w=1200&q=80'
    -- Sài Gòn / HCMC
    WHEN h.city ILIKE '%Hồ Chí Minh%' OR h.city ILIKE '%Ho Chi Minh%' OR h.city ILIKE '%Saigon%' OR h.city ILIKE '%Sài Gòn%' THEN 
        'https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?auto=format&fit=crop&w=1200&q=80'
    -- General default
    ELSE 
        'https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1200&q=80'
END
FROM rooms r
JOIN hotels h ON r.hotel_id = h.hotel_id
WHERE ri.room_id = r.room_id 
  AND (ri.image_url = '/css/room-placeholder.svg' 
       OR ri.image_url = '/css/room-hanoi.svg' 
       OR ri.image_url = '/css/room-danang.svg' 
       OR ri.image_url = '/css/room-dalat.svg' 
       OR ri.image_url = '/css/room-suite.svg' 
       OR ri.image_url = '/css/room-twin.svg'
       OR ri.image_url IS NULL);
