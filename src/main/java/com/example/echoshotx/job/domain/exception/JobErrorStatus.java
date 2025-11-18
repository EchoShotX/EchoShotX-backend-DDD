package com.example.echoshotx.job.domain.exception;

import com.example.echoshotx.shared.exception.payload.code.BaseCode;
import com.example.echoshotx.shared.exception.payload.code.Reason;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum JobErrorStatus implements BaseCode {

    //job error(4200 ~ 4249)
    JOB_NOT_FOUND(HttpStatus.NOT_FOUND, 4200, "찾을 수 없는 Job 정보입니다."),
    JOB_CANNOT_BE_MODIFIED(HttpStatus.BAD_REQUEST, 4201, "현재 상태에서는 Job을 수정할 수 없습니다."),
    JOB_CANNOT_BE_DELETED(HttpStatus.BAD_REQUEST, 4202, "현재 상태에서는 Job을 삭제할 수 없습니다."),;


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
