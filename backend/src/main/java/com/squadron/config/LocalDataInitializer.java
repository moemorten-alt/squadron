package com.squadron.config;

import com.squadron.entity.AppUser;
import com.squadron.entity.UserRole;
import com.squadron.repository.AppUserRepository;
import com.squadron.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Profile("local")
@RequiredArgsConstructor
@Slf4j
public class LocalDataInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final AppUserRepository appUserRepository;
    private final SquadRepository squadRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("classpath:data.sql")
    private Resource dataSql;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (squadRepository.count() == 0) {
            log.info("Seeding local database from data.sql...");
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(dataSql);
            populator.setSeparator(";");
            populator.execute(dataSource);
            log.info("Database seed complete: squads, persons, allocations loaded.");
        } else {
            log.info("Database already contains data, skipping seed.");
        }

        if (!appUserRepository.existsByEmail("admin@squadron.local")) {
            appUserRepository.save(AppUser.builder()
                .email("admin@squadron.local")
                .passwordHash(passwordEncoder.encode("admin123"))
                .userRole(UserRole.ADMIN)
                .active(true)
                .build());
            log.info("Admin user created — login: admin@squadron.local / admin123");
        }
    }
}
