package com.example.demo.infrastructure.exception.payload.code;

public interface BaseCode {
    Reason getReason();
    Reason getReasonHttpStatus();
}
