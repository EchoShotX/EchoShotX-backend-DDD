package com.example.echoshotx.infrastructure.exception.payload.security;

import com.example.echoshotx.infrastructure.exception.payload.code.ErrorStatus;
import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationException extends AuthenticationException {
    public JwtAuthenticationException(ErrorStatus errorStatus){
        super(errorStatus.name());
    }
}