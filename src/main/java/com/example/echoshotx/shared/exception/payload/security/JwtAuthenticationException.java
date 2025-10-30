package com.example.echoshotx.shared.exception.payload.security;

import com.example.echoshotx.shared.exception.payload.code.ErrorStatus;
import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationException extends AuthenticationException {
    public JwtAuthenticationException(ErrorStatus errorStatus){
        super(errorStatus.name());
    }
}