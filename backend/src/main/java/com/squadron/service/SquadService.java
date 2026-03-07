package com.squadron.service;

import com.squadron.dto.AllocationDto;
import com.squadron.dto.CreateSquadRequest;
import com.squadron.dto.SquadDto;
import com.squadron.entity.Squad;
import com.squadron.exception.ResourceNotFoundException;
import com.squadron.repository.AllocationRepository;
import com.squadron.repository.SquadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SquadService {

    private final SquadRepository squadRepository;
    private final AllocationRepository allocationRepository;

    @Transactional(readOnly = true)
    public List<SquadDto> findAll(boolean isAdmin) {
        return squadRepository.findAllByOrderByNameAsc().stream()
                .map(s -> {
                    List<AllocationDto> allocations = allocationRepository.findActiveBySquadId(s.getId()).stream()
                            .map(a -> AllocationDto.from(a, isAdmin))
                            .toList();
                    return SquadDto.from(s, allocations);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SquadDto findById(Long id, boolean isAdmin) {
        Squad s = squadRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found: " + id));
        List<AllocationDto> allocations = s.getAllocations().stream()
                .filter(a -> a.isActive())
                .map(a -> AllocationDto.from(a, isAdmin))
                .sorted((a, b) -> a.personName().compareToIgnoreCase(b.personName()))
                .toList();
        return SquadDto.from(s, allocations);
    }

    @Transactional
    public SquadDto create(CreateSquadRequest req) {
        Squad squad = Squad.builder()
                .name(req.name())
                .description(req.description())
                .build();
        squadRepository.save(squad);
        return SquadDto.from(squad, List.of());
    }

    @Transactional
    public SquadDto update(Long id, CreateSquadRequest req) {
        Squad squad = squadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found: " + id));
        squad.setName(req.name());
        squad.setDescription(req.description());
        squadRepository.save(squad);
        return SquadDto.from(squad, List.of());
    }

    @Transactional
    public void delete(Long id) {
        Squad squad = squadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Squad not found: " + id));
        squadRepository.delete(squad);
    }
}
