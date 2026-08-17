package com.squadron.service;

import com.squadron.dto.PersonDto;
import com.squadron.entity.Person;
import com.squadron.repository.AllocationRepository;
import com.squadron.repository.PersonRepository;
import com.squadron.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the maxAllocation query-filter param added to PersonService.findAll -
 * added to power the MCP server's find_available_people tool.
 */
@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;
    @Mock
    private AllocationRepository allocationRepository;
    @Mock
    private TagRepository tagRepository;

    private PersonService service;

    private Person alice; // 90% allocated
    private Person bob;   // 30% allocated
    private Person carol; // 0% allocated (no allocations at all)

    @BeforeEach
    void setUp() {
        service = new PersonService(personRepository, allocationRepository, tagRepository);

        alice = Person.builder().id(1L).name("Alice").active(true).build();
        bob = Person.builder().id(2L).name("Bob").active(true).build();
        carol = Person.builder().id(3L).name("Carol").active(true).build();

        when(personRepository.findAllActiveWithTags()).thenReturn(List.of(alice, bob, carol));
        when(allocationRepository.findActiveByPersonId(eq(1L))).thenReturn(List.of());
        when(allocationRepository.findActiveByPersonId(eq(2L))).thenReturn(List.of());
        when(allocationRepository.findActiveByPersonId(eq(3L))).thenReturn(List.of());
        when(allocationRepository.sumAllocationPercentByPersonId(eq(1L))).thenReturn(90);
        when(allocationRepository.sumAllocationPercentByPersonId(eq(2L))).thenReturn(30);
        when(allocationRepository.sumAllocationPercentByPersonId(eq(3L))).thenReturn(0);
    }

    @Test
    void noMaxAllocation_returnsEveryone() {
        List<PersonDto> result = service.findAll(true, null);
        assertThat(result).hasSize(3);
    }

    @Test
    void maxAllocation_filtersOutPeopleOverThreshold() {
        List<PersonDto> result = service.findAll(true, 50);
        assertThat(result).extracting(PersonDto::name).containsExactlyInAnyOrder("Bob", "Carol");
    }

    @Test
    void maxAllocation_isInclusiveOfExactMatch() {
        List<PersonDto> result = service.findAll(true, 90);
        assertThat(result).extracting(PersonDto::name).containsExactlyInAnyOrder("Alice", "Bob", "Carol");
    }

    @Test
    void maxAllocation_zero_onlyReturnsFullyUnallocated() {
        List<PersonDto> result = service.findAll(true, 0);
        assertThat(result).extracting(PersonDto::name).containsExactly("Carol");
    }
}
