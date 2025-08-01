package com.example.demo.infrastructure.exception.payload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.*;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseCode {
    //server
    _INTERNAL_SERVER_ERROR(INTERNAL_SERVER_ERROR, 5000, "서버 에러"),

    // general error
    _BAD_REQUEST(BAD_REQUEST, 4000, "잘못된 요청입니다."),
    _UNAUTHORIZED(UNAUTHORIZED, 4001, "로그인이 필요합니다."),
    _FORBIDDEN(FORBIDDEN, 4002, "금지된 요청입니다."),

    //auth error(4050 ~ 4099)
    AUTH_INVALID_REFRESH_TOKEN(UNAUTHORIZED, 4050, "유효하지 않은 리프레시 토큰입니다."),
    AUTH_INVALID_TOKEN(UNAUTHORIZED, 4051, "유효하지 않은 액세스 토큰입니다"),
    AUTH_TOKEN_HAS_EXPIRED(UNAUTHORIZED, 4052, "토큰의 유효기간이 만료되었습니다"),
    AUTH_TOKEN_IS_UNSUPPORTED(UNAUTHORIZED, 4053, "토큰 형식이 jwt와는 다른 형식입니다."),
    AUTH_IS_NULL(UNAUTHORIZED, 4054, "토큰이 null입니다"),
    AUTH_MUST_AUTHORIZED_URI(BAD_REQUEST, 4055, "인증이 필수인 uri입니다."),
    AUTH_ROLE_CANNOT_EXECUTE_URI(BAD_REQUEST, 4056, "해당 인가로는 실행할 수 없는 동작입니다."),
    AUTH_INVALID_AUTHENTICATION(UNAUTHORIZED, 4057, "유효하지 않은 인증 객체입니다."),
    AUTH_ADMIN_PRIVILEGES_REQUIRED(FORBIDDEN, 4003, "관리자의 권한이 필요합니다."),

    //member error(4100 ~ 4149)
    MEMBER_NOT_FOUND(NOT_FOUND, 4100, "찾을 수 없는 유저 정보입니다."),

    // File Error (4201 ~ 4210)
    FILE_READ_ERROR(INTERNAL_SERVER_ERROR, 4201, "파일을 읽는 중 오류가 발생했습니다."),
    FILE_NOT_FOUND(NOT_FOUND, 4202, "파일을 찾을 수 없습니다."),
    FILE_INVALID_EXTENSION(BAD_REQUEST, 4203, "잘못된 형식의 파일입니다."),
    FILE_EXTENSION_NOT_FOUND(NOT_FOUND, 4204, "파일의 형식을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(INTERNAL_SERVER_ERROR, 4205, "파일 업로드에 실패했습니다."),

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
    //server error


}
