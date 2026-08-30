package com.parakore.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "application_sequences")
@IdClass(ApplicationSequenceId.class)
public class ApplicationSequence {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Id
    @Column(name = "financial_year", nullable = false, length = 10)
    private String financialYear;

    @Id
    @Column(name = "module_code", nullable = false, length = 20)
    private String moduleCode;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;
}