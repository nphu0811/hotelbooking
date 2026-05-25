package com.example.demo.controller;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.entity.UserStatus;
import com.example.demo.hoteldata.HotelDataRecord;
import com.example.demo.hoteldata.HotelUpsertService;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@Profile({"local", "test"})
@ConditionalOnProperty(prefix = "app.e2e-fixture", name = "enabled", havingValue = "true")
public class E2eFixtureController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final HotelUpsertService hotelUpsertService;

    public E2eFixtureController(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder,
                                HotelUpsertService hotelUpsertService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.hotelUpsertService = hotelUpsertService;
    }

    @PostMapping("/__e2e__/verify-user")
    public ResponseEntity<String> verifyUser(@RequestParam String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Missing e2e user"));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setEmailVerificationTokenHash(null);
        user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        return ResponseEntity.ok("VERIFIED");
    }

    @PostMapping("/__e2e__/admin")
    public ResponseEntity<String> admin(@RequestParam String email,
                                        @RequestParam String password) {
        Role admin = roleRepository.findByCode("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN", "Hotel administrator")));
        Role superAdmin = roleRepository.findByCode("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("SUPER_ADMIN", "Super administrator")));
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(User::new);
        user.setFullName("E2E Admin");
        user.setEmail(email);
        user.setPhone("0999999999");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.getRoles().clear();
        user.getRoles().add(admin);
        user.getRoles().add(superAdmin);
        userRepository.save(user);
        return ResponseEntity.ok("ADMIN_READY");
    }

    @PostMapping("/__e2e__/overpass-hotel")
    public ResponseEntity<String> overpassHotel(@RequestParam(defaultValue = "") String suffix) {
        String safeSuffix = suffix == null || suffix.isBlank()
                ? "staging-verified"
                : suffix.replaceAll("[^a-zA-Z0-9-]", "-");
        String hotelName = "E2E Overpass Verified Hotel " + safeSuffix;
        HotelDataRecord record = new HotelDataRecord(
                "overpass",
                "node/e2e-" + safeSuffix,
                hotelName,
                "1 E2E Street",
                "Ho Chi Minh City",
                "Ho Chi Minh City",
                "VN",
                new BigDecimal("10.7750000"),
                new BigDecimal("106.7000000"),
                4,
                "+84123456789",
                "https://fixture.example.test/e2e-overpass",
                "https://www.openstreetmap.org/node/e2e-" + safeSuffix,
                null,
                "E2E fixture",
                Map.of("internet_access", "Internet access"),
                "{\"type\":\"node\",\"id\":\"e2e-" + safeSuffix + "\"}"
        );
        hotelUpsertService.upsert(record, false);
        return ResponseEntity.ok("HOTEL_READY");
    }
}
