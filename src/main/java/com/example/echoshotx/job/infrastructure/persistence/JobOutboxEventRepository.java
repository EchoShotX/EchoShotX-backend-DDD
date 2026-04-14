package com.example.echoshotx.job.infrastructure.persistence;

import com.example.echoshotx.job.domain.entity.JobOutboxEvent;
import com.example.echoshotx.job.domain.entity.JobOutboxStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobOutboxEventRepository extends JpaRepository<JobOutboxEvent, Long> {

    List<JobOutboxEvent> findTop100ByStatusAndNextAttemptAtBeforeOrderByIdAsc(
            JobOutboxStatus status,
            LocalDateTime now);

    void deleteByStatusAndCreatedAtBefore(JobOutboxStatus status, LocalDateTime threshold);
}
