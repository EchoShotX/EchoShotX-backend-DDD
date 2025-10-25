package com.example.echoshotx.member.domain.exception;

import com.example.echoshotx.shared.exception.payload.code.BaseCode;
import com.example.echoshotx.shared.exception.payload.code.Reason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@AllArgsConstructor
public enum MemberErrorStatus implements BaseCode {
    //member error(4100 ~ 4149)
    MEMBER_NOT_FOUND(NOT_FOUND, 4100, "찾을 수 없는 유저 정보입니다."),;

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
