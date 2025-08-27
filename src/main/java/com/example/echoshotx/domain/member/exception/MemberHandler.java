package com.example.echoshotx.domain.member.exception;

import com.example.echoshotx.infrastructure.exception.object.general.GeneralException;
import com.example.echoshotx.infrastructure.exception.payload.code.BaseCode;

public class MemberHandler extends GeneralException {
    public MemberHandler(BaseCode code){
        super(code);
    }
}
