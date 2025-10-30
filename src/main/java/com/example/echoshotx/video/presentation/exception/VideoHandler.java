package com.example.echoshotx.video.presentation.exception;

import com.example.echoshotx.shared.exception.object.general.GeneralException;
import com.example.echoshotx.shared.exception.payload.code.BaseCode;

public class VideoHandler extends GeneralException {
    public VideoHandler(BaseCode code) {
        super(code);
    }
}
