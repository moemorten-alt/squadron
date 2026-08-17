package com.squadron.service;

import com.squadron.dto.AllocationDto;
import com.squadron.entity.Allocation;
import com.squadron.entity.DeveloperRole;
import com.squadron.entity.Person;
import com.squadron.entity.Squad;
import com.squadron.entity.Technology;
import com.squadron.repository.AllocationRepository;
import com.squadron.repository.DeveloperRoleRepository;
import com.squadron.repository.PersonRepository;
import com.squadron.repository.SquadRepository;
import com.squadron.repository.TechnologyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the query-filter params added to AllocationService.findAll -
 * added to power the MCP server's search_allocations tool.
 */
@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private AllocationRepository allocationRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private SquadRepository squadRepository;
    @Mock
    private DeveloperRoleRepository developerRoleRepository;
    @Mock
    private TechnologyRepository technologyRepository;

    private AllocationService service;

    private Allocation allocA; // Alice, Squad One, React/Front-end, 50%
    private Allocation allocB; // Bob, Squad Two, Java/Back-end, 30%

    @BeforeEach
    void setUp() {
        service = new AllocationService(allocationRepository, personRepository,
                squadRepository, developerRoleRepository, technologyRepository);

        Person alice = Person.builder().id(1L).name("Alice").build();
        Squad squadOne = Squad.builder().id(10L).name("Squad One").build();
        DeveloperRole frontend = DeveloperRole.builder().id(100L).name("Front-end Developer").build();
        Technology react = Technology.builder().id(200L).name("React").build();

        allocA = Allocation.builder()
                .id(1000L).person(alice).squad(squadOne)
                .roles(Set.of(frontend)).technologies(Set.of(react))
                .allocationPercent(50).active(true).build();

        Person bob = Person.builder().id(2L).name("Bob").build();
        Squad squadTwo = Squad.builder().id(20L).name("Squad Two").build();
        DeveloperRole backend = DeveloperRole.builder().id(101L).name("Back-end Developer").build();
        Technology java = Technology.builder().id(201L).name("Java").build();

        allocB = Allocation.builder()
                .id(1001L).person(bob).squad(squadTwo)
                .roles(Set.of(backend)).technologies(Set.of(java))
                .allocationPercent(30).active(true).build();

        when(allocationRepository.findAllActiveWithDetails()).thenReturn(List.of(allocA, allocB));
    }

    @Test
    void noFilters_returnsEverything() {
        List<AllocationDto> result = service.findAll(true, null, null, null, null, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void filtersByPersonName_caseInsensitiveSubstring() {
        List<AllocationDto> result = service.findAll(true, "ali", null, null, null, null);
        assertThat(result).extracting(AllocationDto::personName).containsExactly("Alice");
    }

    @Test
    void filtersBySquadName_caseInsensitiveSubstring() {
        List<AllocationDto> result = service.findAll(true, null, "squad two", null, null, null);
        assertThat(result).extracting(AllocationDto::personName).containsExactly("Bob");
    }

    @Test
    void filtersByTechnology_caseInsensitive() {
        List<AllocationDto> result = service.findAll(true, null, null, "react", null, null);
        assertThat(result).extracting(AllocationDto::personName).containsExactly("Alice");
    }

    @Test
    void filtersByRole_caseInsensitive() {
        List<AllocationDto> result = service.findAll(true, null, null, null, "back-end developer", null);
        assertThat(result).extracting(AllocationDto::personName).containsExactly("Bob");
    }

    @Test
    void filtersByMinPercent() {
        List<AllocationDto> result = service.findAll(true, null, null, null, null, 40);
        assertThat(result).extracting(AllocationDto::personName).containsExactly("Alice");
    }

    @Test
    void combinedFilters_matchingNothing_returnsEmpty() {
        List<AllocationDto> result = service.findAll(true, "Alice", "Squad Two", null, null, null);
        assertThat(result).isEmpty();
    }
}
