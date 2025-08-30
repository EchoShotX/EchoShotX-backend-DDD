package com.example.echoshotx.domain.video.entity;

import com.example.echoshotx.domain.auditing.entity.BaseTimeEntity;
import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;
import com.example.echoshotx.domain.video.vo.VideoMetadata;
import com.example.echoshotx.domain.video.vo.VideoUrls;
import com.example.echoshotx.infrastructure.exception.object.domain.S3Handler;
import com.example.echoshotx.infrastructure.exception.payload.code.ErrorStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

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

    @Embedded
    private VideoUrls urls;

    public static Video create(
            Long memberId,
            MultipartFile file,
            String s3Key,
            ProcessingType processingType,
            VideoMetadata metadata
    ) {
        // 핵심 도메인 규칙 검증
        validateDomainRules(memberId, file, s3Key, processingType);
        
        return Video.builder()
                .memberId(memberId)
                .originalFileName(file.getOriginalFilename())
                .s3OriginalKey(s3Key)
                .fileSizeBytes(file.getSize())
                .status(VideoStatus.UPLOADED)
                .processingType(processingType)
                .metadata(metadata)
                .urls(VideoUrls.empty())
                .build();
    }

    /**
     * 핵심 도메인 규칙을 검증합니다
     * 도메인이 항상 유효해야 한다는 원칙에 따라 필수적인 비즈니스 규칙을 검증
     */
    private static void validateDomainRules(Long memberId, MultipartFile file, String s3Key, ProcessingType processingType) {
        // 필수 도메인 속성 검증
        if (memberId == null || memberId <= 0) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_MEMBER_ID);
        }
        
        if (file == null) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_NOT_FOUND);
        }
        
        if (s3Key == null || s3Key.trim().isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_S3_KEY);
        }
        
        if (processingType == null) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_PROCESSING_TYPE);
        }
        
        // 파일 크기 0인 경우 (도메인 규칙)
        if (file.getSize() == 0) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_EMPTY_FILE);
        }
        
        // 파일명이 null인 경우 (도메인 규칙)
        if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_FILE_NAME);
        }
    }



    public void validateMember(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_MEMBER_MISMATCH);
        }
    }

    /**
     * 영상에 대한 URL들을 생성합니다
     * 
     * @param thumbnailUrl 썸네일 URL
     * @param streamingUrl 스트리밍 URL
     * @param downloadUrl 다운로드 URL
     * @param expiresAt URL 만료 시간
     */
    public void generateUrls(String thumbnailUrl, String streamingUrl, String downloadUrl, LocalDateTime expiresAt) {
        this.urls = VideoUrls.builder()
                .thumbnailUrl(thumbnailUrl)
                .streamingUrl(streamingUrl)
                .downloadUrl(downloadUrl)
                .expiresAt(expiresAt)
                .build();
    }

    /**
     * 영상에 대한 URL들을 생성합니다 (기본 만료 시간: 1시간)
     * 
     * @param thumbnailUrl 썸네일 URL
     * @param streamingUrl 스트리밍 URL
     * @param downloadUrl 다운로드 URL
     */
    public void generateUrls(String thumbnailUrl, String streamingUrl, String downloadUrl) {
        generateUrls(thumbnailUrl, streamingUrl, downloadUrl, LocalDateTime.now().plusHours(1));
    }

    /**
     * URL들을 초기화합니다
     */
    public void clearUrls() {
        this.urls = VideoUrls.empty();
    }

    /**
     * URL이 유효한지 확인합니다
     */
    public boolean hasValidUrls() {
        return urls != null && urls.hasValidUrls();
    }

    /**
     * URL이 만료되었는지 확인합니다
     */
    public boolean isUrlsExpired() {
        return urls == null || urls.getExpiresAt() == null || 
               urls.getExpiresAt().isBefore(LocalDateTime.now());
    }

    /**
     * URL을 갱신해야 하는지 확인합니다
     */
    public boolean needsUrlRefresh() {
        return urls == null || isUrlsExpired();
    }

    // ========== 도메인 비즈니스 규칙 ==========

    /**
     * 영상 상태를 변경합니다 (도메인 규칙 적용)
     * 
     * @param newStatus 새로운 상태
     * @throws IllegalStateException 상태 전환이 불가능한 경우
     */
    public void changeStatus(VideoStatus newStatus) {
        if (newStatus == null) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_STATUS);
        }
        
        // 상태 전환 규칙 검증
        validateStatusTransition(this.status, newStatus);
        
        this.status = newStatus;
    }

    /**
     * 상태 전환 규칙을 검증합니다
     */
    private void validateStatusTransition(VideoStatus currentStatus, VideoStatus newStatus) {
        // 동일한 상태로의 전환은 허용하지 않음
        if (currentStatus == newStatus) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_SAME_STATUS_TRANSITION);
        }
        
        // 상태 전환 규칙
        switch (currentStatus) {
            case UPLOADED:
                if (newStatus != VideoStatus.PROCESSING && newStatus != VideoStatus.FAILED) {
                    throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_STATUS_TRANSITION);
                }
                break;
            case PROCESSING:
                if (newStatus != VideoStatus.PROCESSED && newStatus != VideoStatus.FAILED) {
                    throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_STATUS_TRANSITION);
                }
                break;
            case PROCESSED:
                if (newStatus == VideoStatus.UPLOADED || newStatus == VideoStatus.PROCESSING) {
                    throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_STATUS_TRANSITION);
                }
                break;
            case FAILED:
                if (newStatus != VideoStatus.PROCESSING) {
                    throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_STATUS_TRANSITION);
                }
                break;
        }
    }

    /**
     * 처리된 영상 키를 설정합니다 (도메인 규칙 적용)
     * 
     * @param processedKey 처리된 영상의 S3 키
     * @throws IllegalStateException 영상이 처리 완료 상태가 아닌 경우
     */
    public void setProcessedKey(String processedKey) {
        if (this.status != VideoStatus.PROCESSED) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_NOT_PROCESSED_STATUS);
        }
        
        if (processedKey == null || processedKey.trim().isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_PROCESSED_KEY);
        }
        
        this.s3ProcessedKey = processedKey;
    }

    /**
     * 썸네일 키를 설정합니다 (도메인 규칙 적용)
     * 
     * @param thumbnailKey 썸네일의 S3 키
     */
    public void setThumbnailKey(String thumbnailKey) {
        if (thumbnailKey != null && thumbnailKey.trim().isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_THUMBNAIL_KEY);
        }
        
        this.s3ThumbnailKey = thumbnailKey;
    }

    /**
     * 영상이 다운로드 가능한 상태인지 확인합니다 (도메인 규칙)
     * 
     * @return 다운로드 가능 여부
     */
    public boolean isDownloadable() {
        return this.status == VideoStatus.PROCESSED && 
               this.s3ProcessedKey != null && 
               !this.s3ProcessedKey.trim().isEmpty();
    }

    /**
     * 영상이 스트리밍 가능한 상태인지 확인합니다 (도메인 규칙)
     * 
     * @return 스트리밍 가능 여부
     */
    public boolean isStreamable() {
        return isDownloadable(); // 다운로드 가능하면 스트리밍도 가능
    }

    /**
     * 영상이 재처리 가능한 상태인지 확인합니다 (도메인 규칙)
     * 
     * @return 재처리 가능 여부
     */
    public boolean canReprocess() {
        return this.status == VideoStatus.FAILED || this.status == VideoStatus.UPLOADED;
    }

}
