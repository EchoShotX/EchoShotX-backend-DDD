package com.example.echoshotx.member.presentation.exception;

import com.example.echoshotx.shared.exception.object.general.GeneralException;
import com.example.echoshotx.shared.exception.payload.code.BaseCode;

public class MemberHandler extends GeneralException {
    public MemberHandler(BaseCode code){
        super(code);
    }
}
