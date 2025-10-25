package com.example.echoshotx.shared.exception.object.domain;

import com.example.echoshotx.shared.exception.object.general.GeneralException;
import com.example.echoshotx.shared.exception.payload.code.BaseCode;

public class S3Handler extends GeneralException {
    public S3Handler(BaseCode code) {
        super(code);
    }
}
