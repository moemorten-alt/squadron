package com.squadron.service;

import com.squadron.dto.AllocationDto;
import com.squadron.dto.CreateAllocationRequest;
import com.squadron.entity.*;
import com.squadron.exception.ResourceNotFoundException;
import com.squadron.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final PersonRepository personRepository;
    private final SquadRepository squadRepository;
    private final DeveloperRoleRepository developerRoleRepository;
    private final TechnologyRepository technologyRepository;

    @Transactional(readOnly = true)
    public List<AllocationDto> findAll(boolean isAdmin, String personName, String squadName,
            String technology, String role, Integer minPercent) {
        return allocationRepository.findAllActiveWithDetails().stream()
                .map(a -> AllocationDto.from(a, isAdmin))
                .filter(dto -> personName == null || containsIgnoreCase(dto.personName(), personName))
                .filter(dto -> squadName == null || containsIgnoreCase(dto.squadName(), squadName))
                .filter(dto -> technology == null
                        || dto.technologies().stream().anyMatch(t -> containsIgnoreCase(t, technology)))
                .filter(dto -> role == null
                        || dto.roles().stream().anyMatch(r -> containsIgnoreCase(r, role)))
                .filter(dto -> minPercent == null
                        || (dto.allocationPercent() != null && dto.allocationPercent() >= minPercent))
                .toList();
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    @Transactional
    public AllocationDto create(CreateAllocationRequest req) {
        Person person = personRepository.findById(req.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + req.personId()));
        Squad squad = squadRepository.findById(req.squadId())
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found: " + req.squadId()));

        List<DeveloperRole> roles = developerRoleRepository.findAllById(req.roleIds());
        List<Technology> techs = req.technologyIds() != null
                ? technologyRepository.findAllById(req.technologyIds())
                : List.of();

        Allocation allocation = Allocation.builder()
                .person(person)
                .squad(squad)
                .roles(new HashSet<>(roles))
                .technologies(new HashSet<>(techs))
                .allocationPercent(req.allocationPercent())
                .publicComment(req.publicComment())
                .adminNote(req.adminNote())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .active(true)
                .build();

        allocationRepository.save(allocation);
        return AllocationDto.from(allocation, true);
    }

    @Transactional
    public AllocationDto update(Long id, CreateAllocationRequest req) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));

        Person person = personRepository.findById(req.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + req.personId()));
        Squad squad = squadRepository.findById(req.squadId())
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found: " + req.squadId()));

        List<DeveloperRole> roles = developerRoleRepository.findAllById(req.roleIds());
        List<Technology> techs = req.technologyIds() != null
                ? technologyRepository.findAllById(req.technologyIds())
                : List.of();

        allocation.setPerson(person);
        allocation.setSquad(squad);
        allocation.setRoles(new HashSet<>(roles));
        allocation.setTechnologies(new HashSet<>(techs));
        allocation.setAllocationPercent(req.allocationPercent());
        allocation.setPublicComment(req.publicComment());
        allocation.setAdminNote(req.adminNote());
        allocation.setStartDate(req.startDate());
        allocation.setEndDate(req.endDate());

        allocationRepository.save(allocation);
        return AllocationDto.from(allocation, true);
    }

    @Transactional
    public void delete(Long id) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));
        allocation.setActive(false);
        allocationRepository.save(allocation);
    }
}
