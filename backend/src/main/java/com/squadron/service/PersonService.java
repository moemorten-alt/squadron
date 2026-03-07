package com.squadron.service;

import com.squadron.dto.AllocationDto;
import com.squadron.dto.CreatePersonRequest;
import com.squadron.dto.PersonDto;
import com.squadron.entity.Person;
import com.squadron.entity.Tag;
import com.squadron.exception.ResourceNotFoundException;
import com.squadron.repository.AllocationRepository;
import com.squadron.repository.PersonRepository;
import com.squadron.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final AllocationRepository allocationRepository;
    private final TagRepository tagRepository;

    @Transactional(readOnly = true)
    public List<PersonDto> findAll(boolean isAdmin) {
        return personRepository.findAllActiveWithTags().stream()
                .map(p -> {
                    List<AllocationDto> allocations = allocationRepository.findActiveByPersonId(p.getId()).stream()
                            .map(a -> AllocationDto.from(a, isAdmin))
                            .toList();
                    Integer total = allocationRepository.sumAllocationPercentByPersonId(p.getId());
                    return PersonDto.from(p, isAdmin, allocations, total);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PersonDto findById(Long id, boolean isAdmin) {
        Person p = personRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + id));
        List<AllocationDto> allocations = p.getAllocations().stream()
                .filter(a -> a.isActive())
                .map(a -> AllocationDto.from(a, isAdmin))
                .toList();
        Integer total = allocationRepository.sumAllocationPercentByPersonId(id);
        return PersonDto.from(p, isAdmin, allocations, total);
    }

    @Transactional
    public PersonDto create(CreatePersonRequest req) {
        Person person = Person.builder()
                .name(req.name())
                .email(req.email())
                .adminNote(req.adminNote())
                .active(true)
                .build();

        if (req.tagIds() != null && !req.tagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(req.tagIds());
            person.setTags(new HashSet<>(tags));
        }

        personRepository.save(person);
        return PersonDto.from(person, true, List.of(), 0);
    }

    @Transactional
    public PersonDto update(Long id, CreatePersonRequest req) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + id));
        person.setName(req.name());
        person.setEmail(req.email());
        person.setAdminNote(req.adminNote());

        if (req.tagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(req.tagIds());
            person.setTags(new HashSet<>(tags));
        }

        personRepository.save(person);
        Integer total = allocationRepository.sumAllocationPercentByPersonId(id);
        return PersonDto.from(person, true, List.of(), total);
    }

    @Transactional
    public void deactivate(Long id) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found: " + id));
        person.setActive(false);
        personRepository.save(person);
    }
}
