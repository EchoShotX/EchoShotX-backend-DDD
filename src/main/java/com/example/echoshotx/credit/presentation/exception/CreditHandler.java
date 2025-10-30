package com.example.echoshotx.credit.presentation.exception;

import com.example.echoshotx.shared.exception.object.general.GeneralException;
import com.example.echoshotx.shared.exception.payload.code.BaseCode;

public class CreditHandler extends GeneralException {
    public CreditHandler(BaseCode code) {
        super(code);
    }
}
