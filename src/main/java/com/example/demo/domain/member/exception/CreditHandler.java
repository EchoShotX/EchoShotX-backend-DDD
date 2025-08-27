package com.example.demo.infrastructure.exception.object.domain;

import com.example.demo.infrastructure.exception.object.general.GeneralException;
import com.example.demo.infrastructure.exception.payload.code.BaseCode;

public class TokenHandler extends GeneralException {
    public TokenHandler(BaseCode code) {
        super(code);
    }
}
