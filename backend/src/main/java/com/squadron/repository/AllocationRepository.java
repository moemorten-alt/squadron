package com.squadron.repository;

import com.squadron.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {

    /*
     * roles and technologies are fetched with LEFT JOIN, not JOIN. They are
     * optional - a Sys Admin or Product Manager allocation routinely has no
     * technologies - and an inner join silently drops every such row. That was
     * not merely a missing list entry: SquadDto derives totalHeadcount and
     * totalAllocationPercent from the list it is handed, so squads staffed
     * entirely by technology-less allocations reported 0 people and 0%.
     *
     * person and squad stay on an inner join: both are mandatory.
     */

    @Query("SELECT a FROM Allocation a JOIN FETCH a.person JOIN FETCH a.squad LEFT JOIN FETCH a.roles LEFT JOIN FETCH a.technologies WHERE a.active = true ORDER BY a.squad.name, a.person.name")
    List<Allocation> findAllActiveWithDetails();

    @Query("SELECT a FROM Allocation a JOIN FETCH a.squad LEFT JOIN FETCH a.roles LEFT JOIN FETCH a.technologies WHERE a.person.id = :personId AND a.active = true")
    List<Allocation> findActiveByPersonId(Long personId);

    @Query("SELECT a FROM Allocation a JOIN FETCH a.person LEFT JOIN FETCH a.roles LEFT JOIN FETCH a.technologies WHERE a.squad.id = :squadId AND a.active = true ORDER BY a.person.name")
    List<Allocation> findActiveBySquadId(Long squadId);

    @Query("SELECT COALESCE(SUM(a.allocationPercent), 0) FROM Allocation a WHERE a.person.id = :personId AND a.active = true")
    Integer sumAllocationPercentByPersonId(Long personId);
}
