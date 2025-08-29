package com.example.echoshotx.domain.video.exception;

import com.example.echoshotx.infrastructure.exception.payload.code.BaseCode;
import com.example.echoshotx.infrastructure.exception.payload.code.Reason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@RequiredArgsConstructor
public enum VideoErrorStatus implements BaseCode {

    // Video Error (4301 ~ 4310)
    VIDEO_PROCESSING_FAILED(INTERNAL_SERVER_ERROR, 4301, "비디오 처리에 실패했습니다."),
    VIDEO_NOT_FOUND(NOT_FOUND, 4302, "비디오를 찾을 수 없습니다."),
    VIDEO_UNSUPPORTED_FORMAT(BAD_REQUEST, 4303, "지원되지 않는 비디오 형식입니다."),
    VIDEO_THUMBNAIL_GENERATION_FAILED(INTERNAL_SERVER_ERROR, 4304, "비디오 썸네일 생성에 실패했습니다."),
    VIDEO_METADATA_EXTRACTION_FAILED(INTERNAL_SERVER_ERROR, 4305, "비디오 메타데이터 추출에 실패했습니다."),
    VIDEO_INVALID_STATUS(BAD_REQUEST, 4306, "잘못된 비디오 상태입니다."),
    VIDEO_MEMBER_MISMATCH(BAD_REQUEST, 4307, "비디오 소유자와 요청한 사용자가 일치하지 않습니다."),
    VIDEO_STORAGE_ERROR(INTERNAL_SERVER_ERROR, 4308, "비디오 저장 중 오류가 발생했습니다."),
    VIDEO_INVALID_FILE_NAME(BAD_REQUEST, 4309, "잘못된 비디오 파일 이름입니다."),;

    private final HttpStatus httpStatus;
    private final Integer code;
    private final String message;

    @Override
    public Reason getReason() {
        return Reason.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public Reason getReasonHttpStatus() {
        return Reason.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
