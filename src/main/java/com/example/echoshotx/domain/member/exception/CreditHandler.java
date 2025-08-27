package com.example.demo.domain.member.exception;

import com.example.demo.infrastructure.exception.object.general.GeneralException;
import com.example.demo.infrastructure.exception.payload.code.BaseCode;

public class CreditHandler extends GeneralException {
    public CreditHandler(BaseCode code) {
        super(code);
    }
}
