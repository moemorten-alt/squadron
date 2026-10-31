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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private String adminToken;
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

        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("admin@test.local")
                .passwordHash(passwordEncoder.encode("password"))
                .userRole(UserRole.ADMIN)
                .active(true)
                .build());
        adminToken = jwtUtil.generateToken(
                new User(admin.getEmail(), admin.getPasswordHash(), List.of(() -> "ROLE_ADMIN")));

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

    @Test
    void admin_canSetLeaverDateOnCreate() throws Exception {
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Leaver\",\"leaverDate\":\"2026-06-30\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaverDate").value("2026-06-30"));
    }

    @Test
    void admin_canClearLeaverDateByOmittingIt() throws Exception {
        // PersonService.update assigns unconditionally, so a null in the request
        // clears the column. This is the documented, intended behaviour.
        //
        // This test MUTATES its subject, so it creates its own person rather than
        // touching the shared leaverId fixture. JUnit does not guarantee method
        // order - reusing the fixture here would intermittently break
        // leaverDate_isVisibleToViewers_unlikeAdminNote depending on run order.
        Person temp = personRepository.save(Person.builder()
                .name("Temp Clear").active(true)
                .leaverDate(LocalDate.of(2026, 1, 1)).build());

        mockMvc.perform(put("/api/persons/" + temp.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Temp Clear\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaverDate").doesNotExist());
    }

    @Test
    void update_withLeaverDateIncluded_preservesIt() throws Exception {
        // What the edit form actually does: round-trip the existing value while
        // changing something else. This is the behaviour users depend on, and the
        // reason PersonForm must always submit leaverDate.
        Person temp = personRepository.save(Person.builder()
                .name("Temp Preserve").active(true)
                .leaverDate(LocalDate.of(2026, 5, 20)).build());

        mockMvc.perform(put("/api/persons/" + temp.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Renamed\",\"leaverDate\":\"2026-05-20\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.leaverDate").value("2026-05-20"));
    }

    @Test
    void futureLeaverDate_isAccepted() throws Exception {
        // A resignation handed in now for a departure months away must not be rejected.
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Future Leaver\",\"leaverDate\":\"2099-12-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leaverDate").value("2099-12-31"));
    }

    @Test
    void viewer_cannotWriteLeaverDate() throws Exception {
        // Authorization is inherited from @PreAuthorize on the controller, not
        // written for this field. This proves the inheritance actually holds.
        mockMvc.perform(post("/api/persons")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nope\",\"leaverDate\":\"2026-01-01\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void leaverDate_isOmittedFromJsonWhenNull() throws Exception {
        // PersonDto is @JsonInclude(NON_NULL): the key is absent, not null.
        // The TypeScript type depends on this.
        Person stayer = personRepository.save(Person.builder()
                .name("Staying Dev").active(true).build());

        mockMvc.perform(get("/api/persons/" + stayer.getId())
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaverDate").doesNotExist());
    }
}
