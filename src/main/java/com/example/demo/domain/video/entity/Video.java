package com.example.demo.domain.video.entity;

import com.example.demo.domain.auditing.entity.BaseTimeEntity;
import com.example.demo.domain.video.vo.VideoMetadata;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Lob
    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String s3OriginalKey;

    @Column
    private String s3ProcessedKey;

    @Column
    private String s3ThumbnailKey;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @Enumerated(EnumType.STRING)
    private ProcessingType processingType;

    @Embedded
    private VideoMetadata metadata;
}
