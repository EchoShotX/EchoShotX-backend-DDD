package com.example.echoshotx.domain.video.entity;

import com.example.echoshotx.domain.auditing.entity.BaseTimeEntity;
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

    public void validateMember(Long memberId) {
        if (!this.memberId.equals(memberId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
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
}
