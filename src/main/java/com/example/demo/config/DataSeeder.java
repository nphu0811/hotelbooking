package com.example.demo.config;

import com.example.demo.entity.Amenity;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.Role;
import com.example.demo.entity.Room;
import com.example.demo.entity.RoomImage;
import com.example.demo.entity.RoomStatus;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.repository.AmenityRepository;
import com.example.demo.repository.HotelRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.RoomRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Profile({"local", "test", "default"})
public class DataSeeder {
    @Bean
    CommandLineRunner seedData(RoleRepository roleRepository,
                               UserRepository userRepository,
                               HotelRepository hotelRepository,
                               RoomRepository roomRepository,
                               AmenityRepository amenityRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            Role userRole = roleRepository.findByCode("USER")
                    .orElseGet(() -> roleRepository.save(new Role("USER", "Default customer role")));
            Role adminRole = roleRepository.findByCode("ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ADMIN", "Hotel administrator")));
            Role superAdminRole = roleRepository.findByCode("SUPER_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("SUPER_ADMIN", "Super administrator")));

            if (!userRepository.existsByEmailIgnoreCase("customer@example.test")) {
                User customer = new User();
                customer.setFullName("Nguyen Minh Demo");
                customer.setEmail("customer@example.test");
                customer.setPhone("0912345678");
                customer.setPasswordHash(passwordEncoder.encode("User@123"));
                customer.setStatus(UserStatus.ACTIVE);
                customer.setEmailVerified(true);
                customer.getRoles().add(userRole);
                userRepository.save(customer);
            }

            if (!userRepository.existsByEmailIgnoreCase("admin@example.test")) {
                User admin = new User();
                admin.setFullName("Admin Demo");
                admin.setEmail("admin@example.test");
                admin.setPhone("0987654321");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setStatus(UserStatus.ACTIVE);
                admin.setEmailVerified(true);
                admin.getRoles().add(adminRole);
                admin.getRoles().add(superAdminRole);
                userRepository.save(admin);
            }

            if (roomRepository.count() > 0) {
                return;
            }

            Amenity wifi = amenityRepository.findByName("Wi-Fi").orElseGet(() -> amenityRepository.save(new Amenity("Wi-Fi", "wifi")));
            Amenity breakfast = amenityRepository.findByName("Bữa sáng").orElseGet(() -> amenityRepository.save(new Amenity("Bữa sáng", "coffee")));
            Amenity pool = amenityRepository.findByName("Hồ bơi").orElseGet(() -> amenityRepository.save(new Amenity("Hồ bơi", "waves")));
            Amenity airport = amenityRepository.findByName("Đưa đón sân bay").orElseGet(() -> amenityRepository.save(new Amenity("Đưa đón sân bay", "car")));

            Hotel hanoi = hotel("Aurora Hanoi Hotel", "Hà Nội", "Hà Nội",
                    "12 Tràng Thi, Hoàn Kiếm", "Khách sạn trung tâm phố cổ, thuận tiện đi bộ tới hồ Hoàn Kiếm.");
            Hotel danang = hotel("Sapphire Danang Bay", "Đà Nẵng", "Đà Nẵng",
                    "88 Võ Nguyên Giáp, Sơn Trà", "Không gian nghỉ dưỡng gần biển Mỹ Khê với dịch vụ gia đình.");
            Hotel dalat = hotel("Pine Garden Dalat", "Đà Lạt", "Lâm Đồng",
                    "5 Hồ Tùng Mậu, Phường 3", "Khách sạn yên tĩnh giữa thành phố, phù hợp cặp đôi và nhóm nhỏ.");
            hotelRepository.saveAll(List.of(hanoi, danang, dalat));

            roomRepository.save(room(hanoi, "Deluxe Phố Cổ", "Deluxe", 2, "32", "1250000",
                    "Phòng sáng, cửa sổ lớn, bàn làm việc và khu tắm đứng.",
                    "Hủy trước 3 ngày hoàn 100%, trước 1-2 ngày hoàn 50%.",
                    "/css/room-hanoi.svg", wifi, breakfast));
            roomRepository.save(room(hanoi, "Family Suite Hồ Gươm", "Suite", 4, "48", "2450000",
                    "Suite hai không gian ngủ, phù hợp gia đình muốn ở gần phố cổ.",
                    "Hủy linh hoạt theo chính sách hệ thống.",
                    "/css/room-suite.svg", wifi, breakfast, airport));
            roomRepository.save(room(danang, "Ocean View King", "King", 2, "38", "1850000",
                    "Ban công nhìn biển, giường king và khu lounge nhỏ.",
                    "Hủy trước 3 ngày hoàn 100%, sau đó áp dụng biểu phí.",
                    "/css/room-danang.svg", wifi, pool, breakfast));
            roomRepository.save(room(danang, "Twin City Comfort", "Twin", 2, "30", "980000",
                    "Phòng twin tối giản, phù hợp khách công tác và nhóm bạn.",
                    "Không hoàn tiền trong ngày check-in.",
                    "/css/room-twin.svg", wifi));
            Room maintenance = room(dalat, "Garden Attic", "Studio", 2, "28", "890000",
                    "Gác mái ấm áp nhìn ra vườn thông.",
                    "Đang bảo trì định kỳ.",
                    "/css/room-dalat.svg", wifi, breakfast);
            maintenance.setStatus(RoomStatus.MAINTENANCE);
            roomRepository.save(maintenance);
        };
    }

    private Hotel hotel(String name, String city, String province, String address, String description) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setCity(city);
        hotel.setProvince(province);
        hotel.setAddress(address);
        hotel.setDescription(description);
        return hotel;
    }

    private Room room(Hotel hotel, String name, String type, int capacity, String area,
                      String price, String description, String policy, String imageUrl,
                      Amenity... amenities) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setName(name);
        room.setRoomType(type);
        room.setCapacity(capacity);
        room.setAreaSqm(new BigDecimal(area));
        room.setPricePerNight(new BigDecimal(price));
        room.setDescription(description);
        room.setCancellationPolicy(policy);
        for (Amenity amenity : amenities) {
            room.getAmenities().add(amenity);
        }
        RoomImage image = new RoomImage();
        image.setRoom(room);
        image.setImageUrl(imageUrl);
        image.setAltText(name);
        image.setPrimary(true);
        room.getImages().add(image);
        return room;
    }
}
