package com.example.echoshotx.job.presentation.exception;

import com.example.echoshotx.shared.exception.object.general.GeneralException;
import com.example.echoshotx.shared.exception.payload.code.BaseCode;

public class JobHandler extends GeneralException {
    public JobHandler(BaseCode code) {
        super(code);
    }
}
