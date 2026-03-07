package com.squadron.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "developer_role")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class DeveloperRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;
}
