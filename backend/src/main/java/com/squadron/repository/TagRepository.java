package com.squadron.repository;

import com.squadron.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findAllByOrderByNameAsc();
    Optional<Tag> findBySlug(String slug);
}
