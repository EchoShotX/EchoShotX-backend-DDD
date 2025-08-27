package com.example.echoshotx.domain.credit.exception;

import com.example.echoshotx.infrastructure.exception.object.general.GeneralException;
import com.example.echoshotx.infrastructure.exception.payload.code.BaseCode;

public class CreditHandler extends GeneralException {
    public CreditHandler(BaseCode code) {
        super(code);
    }
}
