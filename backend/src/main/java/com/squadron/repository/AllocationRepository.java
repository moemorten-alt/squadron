package com.squadron.repository;

import com.squadron.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    @Query("SELECT a FROM Allocation a JOIN FETCH a.person JOIN FETCH a.squad JOIN FETCH a.roles JOIN FETCH a.technologies WHERE a.active = true ORDER BY a.squad.name, a.person.name")
    List<Allocation> findAllActiveWithDetails();

    @Query("SELECT a FROM Allocation a JOIN FETCH a.squad JOIN FETCH a.roles JOIN FETCH a.technologies WHERE a.person.id = :personId AND a.active = true")
    List<Allocation> findActiveByPersonId(Long personId);

    @Query("SELECT a FROM Allocation a JOIN FETCH a.person JOIN FETCH a.roles JOIN FETCH a.technologies WHERE a.squad.id = :squadId AND a.active = true ORDER BY a.person.name")
    List<Allocation> findActiveBySquadId(Long squadId);

    @Query("SELECT COALESCE(SUM(a.allocationPercent), 0) FROM Allocation a WHERE a.person.id = :personId AND a.active = true")
    Integer sumAllocationPercentByPersonId(Long personId);
}
