package com.example.echoshotx.domain.video.entity;

import com.example.echoshotx.domain.auditing.entity.BaseTimeEntity;
import com.example.echoshotx.domain.video.vo.VideoMetadata;
import com.example.echoshotx.infrastructure.exception.object.domain.S3Handler;
import com.example.echoshotx.infrastructure.exception.payload.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "video",
        indexes = {
                @Index(name = "idx_video_member_id", columnList = "member_id")
        }
)
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

    public static Video create(
            Long memberId,
            MultipartFile file,
            String s3Key,
            ProcessingType processingType,
            VideoMetadata metadata
    ) {
        return Video.builder()
                .memberId(memberId)
                .originalFileName(file.getOriginalFilename())
                .s3OriginalKey(s3Key)
                .fileSizeBytes(file.getSize())
                .status(VideoStatus.UPLOADED)
                .processingType(processingType)
                .metadata(metadata)
                .build();
    }

    public void validateMember(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
    }
}
