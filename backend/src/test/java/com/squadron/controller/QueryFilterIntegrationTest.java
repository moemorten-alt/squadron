package com.squadron.controller;

import com.squadron.entity.Allocation;
import com.squadron.entity.AppUser;
import com.squadron.entity.DeveloperRole;
import com.squadron.entity.Person;
import com.squadron.entity.Squad;
import com.squadron.entity.Technology;
import com.squadron.entity.UserRole;
import com.squadron.repository.AllocationRepository;
import com.squadron.repository.AppUserRepository;
import com.squadron.repository.DeveloperRoleRepository;
import com.squadron.repository.PersonRepository;
import com.squadron.repository.SquadRepository;
import com.squadron.repository.TechnologyRepository;
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

import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage (real HTTP layer, via MockMvc) for the query-filter params
 * added to GET /api/persons (maxAllocation) and GET /api/allocations
 * (personId/squadId/technology/role/minPercent) - added to power the MCP server's
 * find_available_people and search_allocations tools.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private SquadRepository squadRepository;
    @Autowired
    private AllocationRepository allocationRepository;
    @Autowired
    private DeveloperRoleRepository developerRoleRepository;
    @Autowired
    private TechnologyRepository technologyRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;

    @BeforeAll
    void setUp() {
        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("query-admin@test.local")
                .passwordHash(passwordEncoder.encode("password"))
                .userRole(UserRole.ADMIN)
                .active(true)
                .build());
        adminToken = jwtUtil.generateToken(new User(admin.getEmail(), admin.getPasswordHash(),
                List.of(() -> "ROLE_ADMIN")));

        Person alice = personRepository.save(Person.builder().name("QF-Alice").active(true).build());
        Person bob = personRepository.save(Person.builder().name("QF-Bob").active(true).build());

        Squad squadOne = squadRepository.save(Squad.builder().name("QF-Squad-One").build());
        Squad squadTwo = squadRepository.save(Squad.builder().name("QF-Squad-Two").build());

        DeveloperRole frontend = developerRoleRepository.save(DeveloperRole.builder().name("QF-Front-end").build());
        DeveloperRole backend = developerRoleRepository.save(DeveloperRole.builder().name("QF-Back-end").build());
        Technology react = technologyRepository.save(Technology.builder().name("QF-React").build());
        Technology java = technologyRepository.save(Technology.builder().name("QF-Java").build());

        allocationRepository.save(Allocation.builder()
                .person(alice).squad(squadOne)
                .roles(Set.of(frontend)).technologies(Set.of(react))
                .allocationPercent(90).active(true).build());
        allocationRepository.save(Allocation.builder()
                .person(bob).squad(squadTwo)
                .roles(Set.of(backend)).technologies(Set.of(java))
                .allocationPercent(20).active(true).build());
    }

    @Test
    void personsList_withMaxAllocation_filtersOutOverThreshold() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .param("maxAllocation", "50")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'QF-Alice')]").isEmpty())
                .andExpect(jsonPath("$[?(@.name == 'QF-Bob')]").isNotEmpty());
    }

    @Test
    void personsList_withoutMaxAllocation_returnsEveryone() throws Exception {
        mockMvc.perform(get("/api/persons")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'QF-Alice')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.name == 'QF-Bob')]").isNotEmpty());
    }

    @Test
    void allocationsSearch_byTechnology_isCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/allocations")
                        .param("technology", "qf-react")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personName == 'QF-Alice')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.personName == 'QF-Bob')]").isEmpty());
    }

    @Test
    void allocationsSearch_bySquadNameAndMinPercent_combines() throws Exception {
        mockMvc.perform(get("/api/allocations")
                        .param("squadName", "squad-two")
                        .param("minPercent", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personName == 'QF-Bob')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.personName == 'QF-Alice')]").isEmpty());
    }

    @Test
    void allocationsSearch_byRole_isCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/allocations")
                        .param("role", "qf-back-end")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personName == 'QF-Bob')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.personName == 'QF-Alice')]").isEmpty());
    }

    @Test
    void allocationsSearch_minPercentAboveEveryAllocation_returnsEmpty() throws Exception {
        mockMvc.perform(get("/api/allocations")
                        .param("minPercent", "95")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personName == 'QF-Alice')]").isEmpty())
                .andExpect(jsonPath("$[?(@.personName == 'QF-Bob')]").isEmpty());
    }
}
