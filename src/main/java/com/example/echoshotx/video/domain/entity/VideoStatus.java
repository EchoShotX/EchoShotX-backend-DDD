package com.example.echoshotx.video.domain.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VideoStatus {
    PENDING_UPLOAD("업로드 대기"),      // presigned URL 발급됨, 아직 업로드 안됨
    UPLOADING("업로드 중"),            // 클라이언트가 업로드 중
    UPLOAD_COMPLETED("업로드 완료"),   // S3 업로드 완료 확인됨
    QUEUED("처리 대기"),              // SQS에 메시지 전송됨
    PROCESSING("AI 처리 중"),         // AI 서버가 처리 시작
    COMPLETED("처리 완료"),           // AI 처리 완료
    FAILED("실패"),                   // 업로드 또는 처리 실패
    ARCHIVED("보관됨");  // 아카이빙됨

    private final String description;

}
