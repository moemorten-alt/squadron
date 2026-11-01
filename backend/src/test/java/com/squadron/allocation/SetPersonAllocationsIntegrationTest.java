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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PUT /api/persons/{id}/allocations sets a person's whole split in one
 * transaction: listed squads are updated or created, unlisted ones removed.
 * It exists because reallocating someone through per-allocation calls took
 * one round trip per squad and could leave them half-updated on failure.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SetPersonAllocationsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PersonRepository personRepository;
    @Autowired private SquadRepository squadRepository;
    @Autowired private AllocationRepository allocationRepository;
    @Autowired private DeveloperRoleRepository developerRoleRepository;
    @Autowired private TechnologyRepository technologyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private TransactionTemplate tx;

    private String adminToken;
    private String viewerToken;
    private Long personId;
    private Long planId, resolutId, imagevaultId, legacyId;
    private Long techLeadRoleId;
    private Long planAllocId, legacyAllocId;

    @BeforeEach
    void setUp() {
        allocationRepository.deleteAll();
        appUserRepository.deleteAll();

        adminToken = tokenFor("split-admin@test.local", UserRole.ADMIN);
        viewerToken = tokenFor("split-viewer@test.local", UserRole.VIEWER);

        String run = String.valueOf(System.nanoTime());
        DeveloperRole techLead = developerRoleRepository.save(DeveloperRole.builder().name("Split Tech Lead " + run).build());
        DeveloperRole dev = developerRoleRepository.save(DeveloperRole.builder().name("Split Dev " + run).build());
        techLeadRoleId = techLead.getId();
        Technology java = technologyRepository.save(Technology.builder().name("Split Java " + run).build());

        planId = squadRepository.save(Squad.builder().name("Split Plan " + run).build()).getId();
        resolutId = squadRepository.save(Squad.builder().name("Split Resolut " + run).build()).getId();
        imagevaultId = squadRepository.save(Squad.builder().name("Split Imagevault " + run).build()).getId();
        legacyId = squadRepository.save(Squad.builder().name("Split Legacy " + run).build()).getId();

        Person diego = personRepository.save(Person.builder().name("Split Diego " + run).active(true).build());
        personId = diego.getId();

        // Plan is his largest allocation, so new squads copy its roles/techs.
        planAllocId = allocationRepository.save(Allocation.builder()
                .person(diego).squad(squadRepository.getReferenceById(planId))
                .roles(new HashSet<>(Set.of(techLead)))
                .technologies(new HashSet<>(Set.of(java)))
                .allocationPercent(60)
                .publicComment("keep me").adminNote("private")
                .startDate(LocalDate.of(2026, 1, 1)).endDate(LocalDate.of(2026, 12, 31))
                .active(true).build()).getId();
        allocationRepository.save(Allocation.builder()
                .person(diego).squad(squadRepository.getReferenceById(resolutId))
                .roles(new HashSet<>(Set.of(dev)))
                .allocationPercent(20).active(true).build());
        legacyAllocId = allocationRepository.save(Allocation.builder()
                .person(diego).squad(squadRepository.getReferenceById(legacyId))
                .roles(new HashSet<>(Set.of(dev)))
                .allocationPercent(20).active(true).build()).getId();
    }

    private String tokenFor(String email, UserRole role) {
        AppUser u = appUserRepository.save(AppUser.builder()
                .email(email).passwordHash(passwordEncoder.encode("password"))
                .userRole(role).active(true).build());
        return jwtUtil.generateToken(new User(u.getEmail(), u.getPasswordHash(),
                List.of(() -> "ROLE_" + role.name())));
    }

    private String body(boolean dryRun, String items) {
        return "{\"dryRun\":" + dryRun + ",\"items\":[" + items + "]}";
    }

    private String diegoSplit() {
        return "{\"squadId\":" + planId + ",\"allocationPercent\":75},"
                + "{\"squadId\":" + resolutId + ",\"allocationPercent\":15},"
                + "{\"squadId\":" + imagevaultId + ",\"allocationPercent\":10}";
    }

    private List<Allocation> active() {
        return tx.execute(s -> {
            List<Allocation> list = allocationRepository.findActiveByPersonId(personId);
            list.forEach(a -> { a.getSquad().getName(); a.getRoles().size(); a.getTechnologies().size(); });
            return list.stream().sorted(Comparator.comparing(a -> a.getSquad().getId())).toList();
        });
    }

    @Test
    void appliesTheWholeSplit_updatingCreatingAndRemoving() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false, diegoSplit())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(false))
                .andExpect(jsonPath("$.totalBefore").value(100))
                .andExpect(jsonPath("$.totalAfter").value(100))
                .andExpect(jsonPath("$.changes[?(@.squadId == " + planId + ")].action").value(hasItem("updated")))
                .andExpect(jsonPath("$.changes[?(@.squadId == " + imagevaultId + ")].action").value(hasItem("created")))
                .andExpect(jsonPath("$.changes[?(@.squadId == " + legacyId + ")].action").value(hasItem("removed")))
                .andExpect(jsonPath("$.after.length()").value(3));

        List<Allocation> after = active();
        assertThat(after).extracting(a -> a.getSquad().getId()).containsExactlyInAnyOrder(planId, resolutId, imagevaultId);
        assertThat(after).extracting(Allocation::getAllocationPercent).containsExactlyInAnyOrder(75, 15, 10);
        assertThat(allocationRepository.findById(legacyAllocId).orElseThrow().isActive()).isFalse();
    }

    @Test
    void updatingPercent_keepsDatesNotesRolesAndTheSameAllocationId() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false, diegoSplit())))
                .andExpect(status().isOk());

        Allocation plan = active().stream().filter(a -> a.getSquad().getId().equals(planId)).findFirst().orElseThrow();
        assertThat(plan.getId()).isEqualTo(planAllocId);
        assertThat(plan.getAllocationPercent()).isEqualTo(75);
        assertThat(plan.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(plan.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(plan.getPublicComment()).isEqualTo("keep me");
        assertThat(plan.getAdminNote()).isEqualTo("private");
        assertThat(plan.getRoles()).extracting(DeveloperRole::getId).containsExactly(techLeadRoleId);
    }

    @Test
    void newSquad_copiesRolesAndTechnologiesFromTheLargestExistingAllocation() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false, diegoSplit())))
                .andExpect(status().isOk());

        Allocation iv = active().stream().filter(a -> a.getSquad().getId().equals(imagevaultId)).findFirst().orElseThrow();
        assertThat(iv.getRoles()).extracting(DeveloperRole::getId).containsExactly(techLeadRoleId);
        assertThat(iv.getTechnologies()).hasSize(1);
    }

    @Test
    void dryRun_reportsTheChangesButSavesNothing() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(true, diegoSplit())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dryRun").value(true))
                .andExpect(jsonPath("$.totalAfter").value(100))
                .andExpect(jsonPath("$.changes[?(@.squadId == " + imagevaultId + ")].action").value(hasItem("created")));

        assertThat(active()).extracting(Allocation::getAllocationPercent).containsExactlyInAnyOrder(60, 20, 20);
        assertThat(active()).hasSize(3);
    }

    @Test
    void totalOver100_isAppliedWithAWarning() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false,
                                "{\"squadId\":" + planId + ",\"allocationPercent\":80},"
                                        + "{\"squadId\":" + resolutId + ",\"allocationPercent\":40}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAfter").value(120))
                .andExpect(jsonPath("$.warnings[0]").exists());
    }

    @Test
    void unknownSquad_rejectsTheWholeRequestAndChangesNothing() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false,
                                "{\"squadId\":" + planId + ",\"allocationPercent\":75},"
                                        + "{\"squadId\":999999,\"allocationPercent\":25}")))
                .andExpect(status().isNotFound());

        assertThat(active()).extracting(Allocation::getAllocationPercent).containsExactlyInAnyOrder(60, 20, 20);
    }

    @Test
    void duplicateSquadInTheRequest_isABadRequest() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false,
                                "{\"squadId\":" + planId + ",\"allocationPercent\":50},"
                                        + "{\"squadId\":" + planId + ",\"allocationPercent\":50}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptySplit_isABadRequest() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false, "")))
                .andExpect(status().isBadRequest());
        assertThat(active()).hasSize(3);
    }

    @Test
    void viewer_isForbidden() throws Exception {
        mockMvc.perform(put("/api/persons/" + personId + "/allocations")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(false, diegoSplit())))
                .andExpect(status().isForbidden());
    }
}
