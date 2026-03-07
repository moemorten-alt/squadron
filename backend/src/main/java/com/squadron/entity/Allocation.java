package com.squadron.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "allocation")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "squad_id", nullable = false)
    private Squad squad;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "allocation_role",
        joinColumns = @JoinColumn(name = "allocation_id"),
        inverseJoinColumns = @JoinColumn(name = "developer_role_id")
    )
    @Builder.Default
    private Set<DeveloperRole> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "allocation_technology",
        joinColumns = @JoinColumn(name = "allocation_id"),
        inverseJoinColumns = @JoinColumn(name = "technology_id")
    )
    @Builder.Default
    private Set<Technology> technologies = new HashSet<>();

    @Column(nullable = false)
    private Integer allocationPercent;

    @Column(columnDefinition = "TEXT")
    private String publicComment;

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
