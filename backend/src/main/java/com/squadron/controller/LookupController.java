package com.squadron.controller;

import com.squadron.entity.DeveloperRole;
import com.squadron.entity.Tag;
import com.squadron.entity.Technology;
import com.squadron.repository.DeveloperRoleRepository;
import com.squadron.repository.TagRepository;
import com.squadron.repository.TechnologyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lookup")
@RequiredArgsConstructor
public class LookupController {

    private final TechnologyRepository technologyRepository;
    private final DeveloperRoleRepository developerRoleRepository;
    private final TagRepository tagRepository;

    @GetMapping("/technologies")
    public ResponseEntity<List<Technology>> technologies() {
        return ResponseEntity.ok(technologyRepository.findAllByOrderByNameAsc());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<DeveloperRole>> roles() {
        return ResponseEntity.ok(developerRoleRepository.findAllByOrderByNameAsc());
    }

    @GetMapping("/tags")
    public ResponseEntity<List<Tag>> tags() {
        return ResponseEntity.ok(tagRepository.findAllByOrderByNameAsc());
    }
}
