package com.example.echoshotx.domain.video.entity;

import com.example.echoshotx.domain.auditing.entity.BaseTimeEntity;
import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;
import com.example.echoshotx.domain.video.vo.ProcessedVideo;
import com.example.echoshotx.domain.video.vo.VideoFile;
import com.example.echoshotx.domain.video.vo.VideoMetadata;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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

    // == 원본 영상 파일, 메타데이터 정보 ==

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fileName", column = @Column(name = "original_file_name", nullable = false)),
            @AttributeOverride(name = "fileSizeBytes", column = @Column(name = "original_file_size_bytes", nullable = false)),
            @AttributeOverride(name = "s3Key", column = @Column(name = "s3_original_key")),
            @AttributeOverride(name = "deletedAt", column = @Column(name = "original_deleted_at"))
    })
    private VideoFile originalFile;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "durationSeconds", column = @Column(name = "original_duration_seconds")),
            @AttributeOverride(name = "width", column = @Column(name = "original_width")),
            @AttributeOverride(name = "height", column = @Column(name = "original_height")),
            @AttributeOverride(name = "codec", column = @Column(name = "original_codec")),
            @AttributeOverride(name = "bitrate", column = @Column(name = "original_bitrate")),
            @AttributeOverride(name = "frameRate", column = @Column(name = "original_frame_rate"))
    })
    private VideoMetadata originalMetadata;
    // ====

    // == 처리된 영상 파일 정보 ==
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "s3Key", column = @Column(name = "s3_processed_key")),
            @AttributeOverride(name = "fileSizeBytes", column = @Column(name = "processed_file_size_bytes"))
    })
    private ProcessedVideo processedVideo;
    // ====

    // == 처리된 영상 메타데이터 ==
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "durationSeconds", column = @Column(name = "processed_duration_seconds")),
            @AttributeOverride(name = "width", column = @Column(name = "processed_width")),
            @AttributeOverride(name = "height", column = @Column(name = "processed_height")),
            @AttributeOverride(name = "codec", column = @Column(name = "processed_codec")),
            @AttributeOverride(name = "bitrate", column = @Column(name = "processed_bitrate")),
            @AttributeOverride(name = "frameRate", column = @Column(name = "processed_frame_rate"))
    })
    private VideoMetadata processedMetadata;

    // ====
    @Column
    private String s3ThumbnailKey;

    // == 스트리밍 필드 ==
    //todo implement
    // ====

    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @Enumerated(EnumType.STRING)
    private ProcessingType processingType;

    // == ai 처리 ==
    @Column(name = "sqs_message_id")
    private String sqsMessageId;

    @Column(name = "ai_job_id")
    private String aiJobId;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    // ====

    // == presigned url ==
    /**
     * 업로드 추적 ID (UUID)
     * Presigned URL 요청 시 생성되며, 업로드 완료 확인에 사용
     */
    @Column(name = "upload_id", unique = true)
    private String uploadId;

    //Presigned URL 만료 시간
    @Column(name = "presigned_url_expires_at")
    private LocalDateTime presignedUrlExpiresAt;

    //업로드 완료 시간
    @Column(name = "upload_completed_at")
    private LocalDateTime uploadCompletedAt;

    // ====

    // == factory methods ==


    // ====


    // business
    // 원본 파일 삭제
    public void markOriginalAsDeleted() {
        if (this.status != VideoStatus.COMPLETED) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_NOT_COMPLETED);
        }

        if (this.processedVideo == null || !this.processedVideo.exists()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_PROCESSED_FILE_NOT_EXISTS);
        }

        this.originalFile = this.originalFile.markAsDeleted();
    }

    // 원본 파일 존재 확인
    public boolean hasOriginalFile() {
        return this.originalFile != null && this.originalFile.exists();
    }


    // 다운로드 가능 여부 확인
    public boolean isDownloadable() {
        return this.status == VideoStatus.COMPLETED
                && this.processedVideo != null
                && this.processedVideo.exists();
    }

    //todo 크레딧 비용 계산
    //todo 비교 정보

    // =======

    // validate
    private static void validatePresignedUploadRules(
            Long memberId,
            String fileName,
            Long fileSizeBytes,
            String s3Key,
            String uploadId
    ) {
        if (memberId == null || memberId <= 0) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_MEMBER_ID);
        }

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_FILE_NAME);
        }

        if (fileSizeBytes == null || fileSizeBytes <= 0) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_EMPTY_FILE);
        }

        if (fileSizeBytes > 500 * 1024 * 1024) {  // todo 임시로 500MB 지정, 추후 변경 가능
            throw new VideoHandler(VideoErrorStatus.VIDEO_FILE_TOO_LARGE);
        }

        if (s3Key == null || s3Key.trim().isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_S3_KEY);
        }

        if (uploadId == null || uploadId.trim().isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_S3_KEY);
        }
    }
}
