package com.example.echoshotx.job.infrastructure.persistence;

import com.example.echoshotx.job.domain.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
}
