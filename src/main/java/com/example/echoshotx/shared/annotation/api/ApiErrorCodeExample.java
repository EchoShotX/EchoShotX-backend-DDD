package com.example.echoshotx.shared.annotation.api;

import com.example.echoshotx.shared.exception.payload.code.ErrorStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodeExample {
    ErrorStatus[] value() default {ErrorStatus._INTERNAL_SERVER_ERROR};
}
