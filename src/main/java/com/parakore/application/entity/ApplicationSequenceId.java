package com.parakore.application.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApplicationSequenceId implements Serializable {

    private String tenantId;

    private String financialYear;

    private String moduleCode;
}