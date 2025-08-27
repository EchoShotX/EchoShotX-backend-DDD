package com.example.echoshotx.domain.credit.exception;

import com.example.echoshotx.infrastructure.exception.payload.code.BaseCode;
import com.example.echoshotx.infrastructure.exception.payload.code.Reason;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Getter
@AllArgsConstructor
public enum CreditErrorStatus implements BaseCode {
    //Credit error (4211 ~ 4220)
    CREDIT_NOT_ENOUGH(BAD_REQUEST, 4211, "크레딧이 부족합니다."),
    CREDIT_EXCEED_LIMIT(BAD_REQUEST, 4212, "크레딧 한도를 초과했습니다."),
    CREDIT_TRANSACTION_FAILED(INTERNAL_SERVER_ERROR, 4213, "크레딧 거래에 실패했습니다."),
    CREDIT_NOT_VALID(BAD_REQUEST, 4214, "유효하지 않은 크레딧 정보입니다."),
    CREDIT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, 4215, "크레딧 사용자를 찾을 수 없습니다."),
    CREDIT_NOT_VALID_USAGE(BAD_REQUEST, 4216, "유효하지 않은 크레딧 사용 정보입니다."),
    CREDIT_INVALID_QUERY_LIMIT(BAD_REQUEST, 4217, "조회 개수는 0보다 커야 합니다."),
    CREDIT_INVALID_DATE_RANGE(BAD_REQUEST, 4218, "시작 날짜는 종료 날짜보다 이전이어야 합니다."),
    ;

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
