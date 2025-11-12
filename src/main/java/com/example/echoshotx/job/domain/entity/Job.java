package com.example.echoshotx.job.domain.entity;

import com.example.echoshotx.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Job extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String s3Key;

    @Enumerated(EnumType.STRING)
    private JobStatus status;

    private String taskType;

    public static Job create(String s3Key, String taskType) {
        return Job.builder()
                .s3Key(s3Key)
                .taskType(taskType)
                .status(JobStatus.REQUESTED)
                .build();
    }

    public void markPublished() {
        this.status = JobStatus.PUBLISHED;
    }
}
