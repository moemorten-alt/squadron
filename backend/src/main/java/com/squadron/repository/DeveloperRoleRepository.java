package com.squadron.repository;

import com.squadron.entity.DeveloperRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeveloperRoleRepository extends JpaRepository<DeveloperRole, Long> {
    List<DeveloperRole> findAllByOrderByNameAsc();
    Optional<DeveloperRole> findByName(String name);
}
