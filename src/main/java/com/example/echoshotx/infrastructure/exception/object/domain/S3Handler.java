package com.example.echoshotx.infrastructure.exception.object.domain;

import com.example.echoshotx.infrastructure.exception.object.general.GeneralException;
import com.example.echoshotx.infrastructure.exception.payload.code.BaseCode;

public class S3Handler extends GeneralException {
    public S3Handler(BaseCode code) {
        super(code);
    }
}
