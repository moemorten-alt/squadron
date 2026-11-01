package com.squadron.allocation;

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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An allocation is not required to have technologies - plenty of real roles
 * (Sys Admin, Product Manager) carry none. The detail queries fetch roles and
 * technologies with JOIN FETCH, which is an INNER join, so an allocation with
 * an empty collection silently disappears from any endpoint built on them.
 *
 * That is not a cosmetic omission: SquadDto derives totalHeadcount and
 * totalAllocationPercent from whatever list it is handed, so a squad staffed
 * entirely by technology-less allocations reports zero people and zero percent
 * while being fully booked.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AllocationFetchIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private SquadRepository squadRepository;
    @Autowired private AllocationRepository allocationRepository;
    @Autowired private DeveloperRoleRepository developerRoleRepository;
    @Autowired private TechnologyRepository technologyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    private String token;
    private Long techlessSquadId;
    private Long mixedSquadId;

    @BeforeAll
    void setUp() {
        appUserRepository.deleteAll();
        allocationRepository.deleteAll();

        AppUser admin = appUserRepository.save(AppUser.builder()
                .email("fetch-admin@test.local")
                .passwordHash(passwordEncoder.encode("password"))
                .userRole(UserRole.ADMIN)
                .active(true)
                .build());
        token = jwtUtil.generateToken(
                new User(admin.getEmail(), admin.getPasswordHash(), java.util.List.of(() -> "ROLE_ADMIN")));

        DeveloperRole sysAdmin = developerRoleRepository.save(
                DeveloperRole.builder().name("Fetch Sys Admin").build());
        Technology java = technologyRepository.save(
                Technology.builder().name("Fetch Java").build());

        // A squad whose every allocation has roles but NO technologies.
        Squad techless = squadRepository.save(Squad.builder().name("Fetch Techless Squad").build());
        techlessSquadId = techless.getId();
        Person p1 = personRepository.save(Person.builder().name("Fetch Techless One").active(true).build());
        Person p2 = personRepository.save(Person.builder().name("Fetch Techless Two").active(true).build());
        allocationRepository.save(Allocation.builder()
                .person(p1).squad(techless)
                .roles(new HashSet<>(Set.of(sysAdmin)))
                .technologies(new HashSet<>())
                .allocationPercent(60).active(true).build());
        allocationRepository.save(Allocation.builder()
                .person(p2).squad(techless)
                .roles(new HashSet<>(Set.of(sysAdmin)))
                .technologies(new HashSet<>())
                .allocationPercent(40).active(true).build());

        // A squad mixing one allocation with technologies and one without, so a
        // partially-correct total is distinguishable from a fully correct one.
        Squad mixed = squadRepository.save(Squad.builder().name("Fetch Mixed Squad").build());
        mixedSquadId = mixed.getId();
        Person p3 = personRepository.save(Person.builder().name("Fetch Mixed With").active(true).build());
        Person p4 = personRepository.save(Person.builder().name("Fetch Mixed Without").active(true).build());
        allocationRepository.save(Allocation.builder()
                .person(p3).squad(mixed)
                .roles(new HashSet<>(Set.of(sysAdmin)))
                .technologies(new HashSet<>(Set.of(java)))
                .allocationPercent(50).active(true).build());
        allocationRepository.save(Allocation.builder()
                .person(p4).squad(mixed)
                .roles(new HashSet<>(Set.of(sysAdmin)))
                .technologies(new HashSet<>())
                .allocationPercent(30).active(true).build());
    }

    private String auth() {
        return "Bearer " + token;
    }

    @Test
    void allocationsWithoutTechnologies_appearInTheAllocationList() throws Exception {
        mockMvc.perform(get("/api/allocations").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personName == 'Fetch Techless One')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.personName == 'Fetch Techless Two')]").isNotEmpty());
    }

    @Test
    void allocationsWithTechnologies_stillAppearInTheAllocationList() throws Exception {
        // Guards against "fixing" the inner join by dropping the fetch entirely.
        mockMvc.perform(get("/api/allocations").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personName == 'Fetch Mixed With')]").isNotEmpty());
    }

    @Test
    void technologiesAreStillPopulated_notSilentlyEmptied() throws Exception {
        // The fetch must keep loading technologies; a LEFT JOIN that lost them
        // would make every allocation look technology-less and pass test one.
        mockMvc.perform(get("/api/allocations").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personName == 'Fetch Mixed With')].technologies[0]")
                        .value(org.hamcrest.Matchers.hasItem("Fetch Java")));
    }

    @Test
    void rolesAreStillPopulated() throws Exception {
        mockMvc.perform(get("/api/allocations").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.personName == 'Fetch Techless One')].roles[0]")
                        .value(org.hamcrest.Matchers.hasItem("Fetch Sys Admin")));
    }

    @Test
    void squadList_countsTechnologylessAllocationsInHeadcountAndPercent() throws Exception {
        // The headline failure: a fully-staffed squad reporting as empty.
        mockMvc.perform(get("/api/squads").header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Fetch Techless Squad')].totalHeadcount")
                        .value(org.hamcrest.Matchers.hasItem(2)))
                .andExpect(jsonPath("$[?(@.name == 'Fetch Techless Squad')].totalAllocationPercent")
                        .value(org.hamcrest.Matchers.hasItem(100)));
    }

    @Test
    void squadList_totalsMatchTheSquadDetailView() throws Exception {
        // The list view and the detail view read allocations through different
        // paths; they must not disagree.
        mockMvc.perform(get("/api/squads").header("Authorization", auth()))
                .andExpect(jsonPath("$[?(@.name == 'Fetch Mixed Squad')].totalHeadcount")
                        .value(org.hamcrest.Matchers.hasItem(2)))
                .andExpect(jsonPath("$[?(@.name == 'Fetch Mixed Squad')].totalAllocationPercent")
                        .value(org.hamcrest.Matchers.hasItem(80)));

        mockMvc.perform(get("/api/squads/" + mixedSquadId).header("Authorization", auth()))
                .andExpect(jsonPath("$.totalHeadcount").value(2))
                .andExpect(jsonPath("$.totalAllocationPercent").value(80));
    }

    @Test
    void squadDetail_forATechnologylessSquad_isCorrect() throws Exception {
        mockMvc.perform(get("/api/squads/" + techlessSquadId).header("Authorization", auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHeadcount").value(2))
                .andExpect(jsonPath("$.totalAllocationPercent").value(100));
    }
}
