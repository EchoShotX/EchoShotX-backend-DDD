package com.example.echoshotx.shared.aws.validator;

import com.example.echoshotx.shared.exception.object.domain.S3Handler;
import com.example.echoshotx.shared.exception.payload.code.ErrorStatus;

import java.util.Arrays;
import java.util.List;

public class S3Validator {

    private static final long MAX_UPLOAD_SIZE_BYTES = 500 * 1024 * 1024; // 500MB

    public static void validateUploadSize(long contentLength) {
        if(contentLength <= 0) {
            throw new S3Handler(ErrorStatus.FILE_INVALID_EXTENSION);
        }
        if(contentLength > MAX_UPLOAD_SIZE_BYTES) {
            throw new S3Handler(ErrorStatus.FILE_UPLOAD_FAILED);
        }
    }

    public static void validateVideoContentType(String contentType) {
        if (contentType == null) {
            throw new S3Handler(ErrorStatus.FILE_INVALID_EXTENSION);
        }

        List<String> allowedContentTypes = Arrays.asList(
                "video/mp4",
                "video/avi",
                "video/quicktime",
                "video/x-ms-wmv",
                "video/x-flv",
                "video/x-matroska"
        );

        if(!allowedContentTypes.contains(contentType.toLowerCase())) {
            throw new S3Handler(ErrorStatus.FILE_INVALID_EXTENSION);
        }
    }

}
