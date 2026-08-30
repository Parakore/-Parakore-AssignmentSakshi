package com.parakore.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "application_transitions",
        indexes = {
                @Index(
                        name = "idx_transitions_application",
                        columnList = "application_id, created_time"
                )
        }
)
public class ApplicationTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "application_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transition_application")
    )
    private Application application;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "action", nullable = false, length = 30)
    private String action;

    @Column(name = "to_status", nullable = false, length = 30)
    private String toStatus;

    @Column(name = "actor_uuid", nullable = false, length = 100)
    private String actorUuid;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Column(name = "actor_role", nullable = false, length = 30)
    private String actorRole;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
}