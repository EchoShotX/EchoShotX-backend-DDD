package com.example.echoshotx.job.infrastructure.persistence;

import com.example.echoshotx.job.domain.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByVideoId(Long videoId);
}
