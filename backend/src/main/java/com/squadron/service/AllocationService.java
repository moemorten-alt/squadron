package com.squadron.service;

import com.squadron.dto.AllocationDto;
import com.squadron.dto.CreateAllocationRequest;
import com.squadron.dto.SetPersonAllocationsRequest;
import com.squadron.dto.SetPersonAllocationsResult;
import com.squadron.entity.*;
import com.squadron.exception.BadRequestException;
import com.squadron.exception.ResourceNotFoundException;
import com.squadron.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final PersonRepository personRepository;
    private final SquadRepository squadRepository;
    private final DeveloperRoleRepository developerRoleRepository;
    private final TechnologyRepository technologyRepository;

    @Transactional(readOnly = true)
    public List<AllocationDto> findAll(boolean isAdmin) {
        return allocationRepository.findAllActiveWithDetails().stream()
                .map(a -> AllocationDto.from(a, isAdmin))
                .toList();
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

    /**
     * Set a person's whole split in one transaction. Everything is validated
     * before anything is changed, so a bad item rejects the request with the
     * person untouched, and a dry run can return the same report without
     * writing.
     *
     * Existing allocations are updated in place: only the percent (and roles /
     * technologies, when given) change, so dates, notes and the allocation id
     * survive. A new squad without roleIds copies roles and technologies from
     * the person's largest current allocation. Squads not listed are removed
     * (soft delete, as delete() does).
     */
    @Transactional
    public SetPersonAllocationsResult setPersonAllocations(Long personId, SetPersonAllocationsRequest req) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + personId));

        List<Allocation> current = allocationRepository.findActiveByPersonId(personId).stream()
                .sorted(Comparator.comparing(Allocation::getId))
                .toList();
        List<SetPersonAllocationsResult.Entry> before = current.stream()
                .map(a -> entry(a.getId(), a.getSquad(), a.getAllocationPercent(), a.getRoles()))
                .toList();

        Set<Long> requested = new HashSet<>();
        for (SetPersonAllocationsRequest.Item item : req.items()) {
            if (!requested.add(item.squadId())) {
                throw new BadRequestException("Squad " + item.squadId() + " is listed more than once");
            }
        }

        Map<Long, List<Allocation>> currentBySquad = current.stream()
                .collect(Collectors.groupingBy(a -> a.getSquad().getId()));
        Allocation copySource = current.stream()
                .max(Comparator.comparing(Allocation::getAllocationPercent)
                        .thenComparing(Allocation::getId, Comparator.reverseOrder()))
                .orElse(null);

        // ── Plan: resolve and validate every item before touching anything ──
        record Planned(SetPersonAllocationsRequest.Item item, Squad squad, Allocation existing,
                       Set<DeveloperRole> roles, Set<Technology> techs) {}
        List<Planned> plan = new ArrayList<>();
        for (SetPersonAllocationsRequest.Item item : req.items()) {
            Squad squad = squadRepository.findById(item.squadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Squad not found: " + item.squadId()));
            List<Allocation> inSquad = currentBySquad.getOrDefault(squad.getId(), List.of());
            if (inSquad.size() > 1) {
                throw new BadRequestException(person.getName() + " has " + inSquad.size()
                        + " active allocations in " + squad.getName() + "; remove the duplicates first");
            }
            Allocation existing = inSquad.isEmpty() ? null : inSquad.get(0);

            Set<DeveloperRole> roles = item.roleIds() != null ? resolveRoles(item.roleIds())
                    : existing != null ? existing.getRoles()
                    : copySource != null ? copySource.getRoles()
                    : null;
            if (roles == null) {
                throw new BadRequestException(person.getName() + " has no allocation to copy roles from; give roleIds for "
                        + squad.getName());
            }
            Set<Technology> techs = item.technologyIds() != null ? resolveTechs(item.technologyIds())
                    : existing != null ? existing.getTechnologies()
                    : copySource != null ? copySource.getTechnologies()
                    : Set.of();
            plan.add(new Planned(item, squad, existing, roles, techs));
        }
        List<Allocation> toRemove = current.stream()
                .filter(a -> !requested.contains(a.getSquad().getId()))
                .toList();

        // ── Apply (unless dry run) and report ──
        List<SetPersonAllocationsResult.Change> changes = new ArrayList<>();
        List<SetPersonAllocationsResult.Entry> after = new ArrayList<>();
        for (Planned p : plan) {
            int pct = p.item().allocationPercent();
            Allocation a = p.existing();
            if (a == null) {
                Long newId = null;
                if (!req.dryRun()) {
                    newId = allocationRepository.save(Allocation.builder()
                            .person(person).squad(p.squad())
                            .roles(new HashSet<>(p.roles()))
                            .technologies(new HashSet<>(p.techs()))
                            .allocationPercent(pct)
                            .active(true)
                            .build()).getId();
                }
                changes.add(new SetPersonAllocationsResult.Change("created", newId, p.squad().getId(), p.squad().getName(), null, pct));
                after.add(entry(newId, p.squad(), pct, p.roles()));
            } else {
                boolean changed = a.getAllocationPercent() != pct
                        || !ids(a.getRoles()).equals(ids(p.roles()))
                        || !ids(a.getTechnologies()).equals(ids(p.techs()));
                int from = a.getAllocationPercent();
                if (changed && !req.dryRun()) {
                    a.setAllocationPercent(pct);
                    a.setRoles(new HashSet<>(p.roles()));
                    a.setTechnologies(new HashSet<>(p.techs()));
                    allocationRepository.save(a);
                }
                changes.add(new SetPersonAllocationsResult.Change(changed ? "updated" : "unchanged",
                        a.getId(), p.squad().getId(), p.squad().getName(), from, pct));
                after.add(entry(a.getId(), p.squad(), pct, p.roles()));
            }
        }
        for (Allocation a : toRemove) {
            if (!req.dryRun()) {
                a.setActive(false);
                allocationRepository.save(a);
            }
            changes.add(new SetPersonAllocationsResult.Change("removed", a.getId(), a.getSquad().getId(),
                    a.getSquad().getName(), a.getAllocationPercent(), null));
        }

        int totalBefore = before.stream().mapToInt(SetPersonAllocationsResult.Entry::allocationPercent).sum();
        int totalAfter = after.stream().mapToInt(SetPersonAllocationsResult.Entry::allocationPercent).sum();
        List<String> warnings = totalAfter > 100
                ? List.of(person.getName() + " would be allocated " + totalAfter + "%, over 100%")
                : List.of();

        return new SetPersonAllocationsResult(person.getId(), person.getName(), req.dryRun(),
                totalBefore, totalAfter, before, after, changes, warnings);
    }

    private Set<DeveloperRole> resolveRoles(List<Long> ids) {
        List<DeveloperRole> found = developerRoleRepository.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            throw new BadRequestException("Unknown role id in " + ids);
        }
        return new HashSet<>(found);
    }

    private Set<Technology> resolveTechs(List<Long> ids) {
        List<Technology> found = technologyRepository.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            throw new BadRequestException("Unknown technology id in " + ids);
        }
        return new HashSet<>(found);
    }

    private static Set<Long> ids(Set<?> entities) {
        return entities.stream().map(e -> e instanceof DeveloperRole r ? r.getId() : ((Technology) e).getId())
                .collect(Collectors.toSet());
    }

    private static SetPersonAllocationsResult.Entry entry(Long id, Squad squad, int pct, Set<DeveloperRole> roles) {
        return new SetPersonAllocationsResult.Entry(id, squad.getId(), squad.getName(), pct,
                roles.stream().map(DeveloperRole::getName).sorted().toList());
    }

    @Transactional
    public void delete(Long id) {
        Allocation allocation = allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found: " + id));
        allocation.setActive(false);
        allocationRepository.save(allocation);
    }
}
