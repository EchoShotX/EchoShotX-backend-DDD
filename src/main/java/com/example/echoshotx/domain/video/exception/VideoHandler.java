package com.example.echoshotx.domain.video.exception;

import com.example.echoshotx.infrastructure.exception.object.general.GeneralException;
import com.example.echoshotx.infrastructure.exception.payload.code.BaseCode;

public class VideoHandler extends GeneralException {
    public VideoHandler(BaseCode code) {
        super(code);
    }
}
