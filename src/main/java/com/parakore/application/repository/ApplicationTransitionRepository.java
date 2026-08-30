package com.parakore.application.repository;

import com.parakore.application.entity.ApplicationTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationTransitionRepository
        extends JpaRepository<ApplicationTransition, Long> {

    List<ApplicationTransition> findByApplicationIdOrderByCreatedTimeAsc(
            Long applicationId
    );
}