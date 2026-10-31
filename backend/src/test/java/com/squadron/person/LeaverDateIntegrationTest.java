package com.squadron.person;

import com.squadron.entity.AppUser;
import com.squadron.entity.Person;
import com.squadron.entity.UserRole;
import com.squadron.repository.AppUserRepository;
import com.squadron.repository.PersonRepository;
import com.squadron.security.JwtUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaverDateIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String viewerToken;
    private Long leaverId;

    @BeforeAll
    void setUp() {
        appUserRepository.deleteAll();

        AppUser viewer = appUserRepository.save(AppUser.builder()
                .email("viewer@test.local")
                .passwordHash(passwordEncoder.encode("password"))
                .userRole(UserRole.VIEWER)
                .active(true)
                .build());
        viewerToken = jwtUtil.generateToken(
                new User(viewer.getEmail(), viewer.getPasswordHash(), Collections.emptyList()));

        Person leaver = personRepository.save(Person.builder()
                .name("Departing Dev")
                .email("departing@test.local")
                .active(true)
                .leaverDate(LocalDate.of(2026, 3, 14))
                .build());
        leaverId = leaver.getId();
    }

    @Test
    void leaverDate_isVisibleToViewers_unlikeAdminNote() throws Exception {
        // leaverDate deliberately sits OUTSIDE includeAdminFields. It is the field
        // most likely to be "tidied" into the admin-only branch beside adminNote,
        // so this pins the intent down.
        mockMvc.perform(get("/api/persons/" + leaverId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaverDate").value("2026-03-14"));
    }

    @Test
    void leaverDate_serialisesAsAnIsoDateString() throws Exception {
        mockMvc.perform(get("/api/persons/" + leaverId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaverDate").isString());
    }
}
