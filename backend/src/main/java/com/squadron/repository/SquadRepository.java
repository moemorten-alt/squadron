package com.squadron.repository;

import com.squadron.entity.Squad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SquadRepository extends JpaRepository<Squad, Long> {

    List<Squad> findAllByOrderByNameAsc();

    @Query("SELECT DISTINCT s FROM Squad s LEFT JOIN FETCH s.allocations a LEFT JOIN FETCH a.person LEFT JOIN FETCH a.roles LEFT JOIN FETCH a.technologies WHERE s.id = :id")
    Optional<Squad> findByIdWithDetails(Long id);
}
