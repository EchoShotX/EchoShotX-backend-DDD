package com.example.echoshotx.domain.video.vo;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoFile {

    private String fileName;
    private Long fileSizeBytes;
    private String s3Key; //S3 키
    private LocalDateTime deletedAt; //S3에서 삭제된 시간

    //business

    //S3에 파일이 존재하는지 확인
    public boolean exists() {
        return s3Key != null && deletedAt == null;
    }

    // 삭제 처리
    public VideoFile markAsDeleted() {
        return VideoFile.builder()
                .fileName(this.fileName)
                .fileSizeBytes(this.fileSizeBytes)
                .s3Key(null)  // S3 키 제거
                .deletedAt(LocalDateTime.now())
                .build();
    }

    //create method
    public static VideoFile createWithoutS3(String fileName, Long fileSizeBytes) {
        return VideoFile.builder()
                .fileName(fileName)
                .fileSizeBytes(fileSizeBytes)
                .s3Key(null)
                .deletedAt(null)
                .build();
    }
}
