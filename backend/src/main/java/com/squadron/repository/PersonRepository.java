package com.squadron.repository;

import com.squadron.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findAllByActiveTrue();

    @Query("SELECT DISTINCT p FROM Person p LEFT JOIN FETCH p.tags LEFT JOIN FETCH p.allocations a LEFT JOIN FETCH a.squad LEFT JOIN FETCH a.roles LEFT JOIN FETCH a.technologies WHERE p.id = :id")
    Optional<Person> findByIdWithDetails(Long id);

    @Query("SELECT DISTINCT p FROM Person p LEFT JOIN FETCH p.tags WHERE p.active = true ORDER BY p.name")
    List<Person> findAllActiveWithTags();
}
