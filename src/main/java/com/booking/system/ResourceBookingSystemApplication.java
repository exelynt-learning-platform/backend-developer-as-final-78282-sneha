package com.booking.system;

import com.booking.system.model.Role;
import com.booking.system.model.User;
import com.booking.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Resource Booking System — Spring Boot Application Entry Point.
 *
 * Seeds default ADMIN and USER accounts on first startup if they don't exist.
 *   ADMIN: username=admin / password=admin123
 *   USER:  username=user  / password=user123
 *
 * Seeding is skipped during the "test" profile — tests manage their own data.
 */
@SpringBootApplication
@RequiredArgsConstructor
@Slf4j
public class ResourceBookingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceBookingSystemApplication.class, args);
    }

    @Bean
    @Profile("!test")  // Do NOT seed during tests — tests manage their own users
    CommandLineRunner seedData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .email("admin@booking.com")
                        .role(Role.ROLE_ADMIN)
                        .build();
                userRepository.save(admin);
                log.info("Default ADMIN user created: username=admin, password=admin123");
            }

            if (!userRepository.existsByUsername("user")) {
                User user = User.builder()
                        .username("user")
                        .password(passwordEncoder.encode("user123"))
                        .email("user@booking.com")
                        .role(Role.ROLE_USER)
                        .build();
                userRepository.save(user);
                log.info("Default USER created: username=user, password=user123");
            }
        };
    }
}
