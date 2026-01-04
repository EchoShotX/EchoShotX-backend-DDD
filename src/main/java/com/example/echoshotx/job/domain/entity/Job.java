package com.example.echoshotx.job.domain.entity;

import com.example.echoshotx.shared.common.BaseTimeEntity;
import com.example.echoshotx.video.domain.entity.ProcessingType;
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

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "video_id")
    private Long videoId;

    private ProcessingType processingType;

    public static Job create(Long memberId, Long videoId, String s3Key, ProcessingType processingType) {
        return Job.builder()
                .memberId(memberId)
                .videoId(videoId)
                .s3Key(s3Key)
                .processingType(processingType)
                .status(JobStatus.REQUESTED)
                .build();
    }

    public void markPublished() {
        this.status = JobStatus.PUBLISHED;
    }

    public void markFailed() {
        this.status = JobStatus.FAILED;
    }
}
