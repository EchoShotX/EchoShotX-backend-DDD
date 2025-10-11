package com.example.echoshotx.domain.video.validator;

import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Video 도메인의 입력 검증을 담당하는 Validator
 */
@Component
public class VideoValidator {

    // 파일 형식 및 크기 제한 상수
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v"
    );

    // 수정 가능
    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    private static final long MIN_FILE_SIZE_BYTES = 1024 * 1024; // 1MB

    public void validateVideoFile(MultipartFile file) {
        // 파일 존재 여부 (기술적 검증)
        if (file == null || file.isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_NOT_FOUND);
        }

        // 파일명 형식 검증 (기술적 검증)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_INVALID_FILE_NAME);
        }

        // 파일 확장자 검증 (입력 형식 검증)
        validateFileExtension(originalFilename);

        // 파일 크기 제한 검증 (입력 형식 검증)
        validateFileSize(file.getSize());
    }

    public void validateFileExtension(String filename) {
        String extension = getFileExtension(filename);
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_UNSUPPORTED_FORMAT);
        }
    }

    public void validateFileSize(long fileSizeBytes) {
        if (fileSizeBytes < MIN_FILE_SIZE_BYTES) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_FILE_TOO_SMALL);
        }

        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_FILE_TOO_LARGE);
        }
    }

    public String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_EXTENSION_NOT_FOUND);
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    public boolean isValidVideoFormat(String filename) {
        if (filename == null) {
            return false;
        }
        try {
            String extension = getFileExtension(filename);
            return ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase());
        } catch (Exception e) {
            return false;
        }
    }


    public boolean isValidFileSize(long fileSizeBytes) {
        return fileSizeBytes >= MIN_FILE_SIZE_BYTES && fileSizeBytes <= MAX_FILE_SIZE_BYTES;
    }

    public String getFileValidationRules() {
        return String.format(
                "파일 형식: %s, 최소 크기: %d MB, 최대 크기: %d MB",
                String.join(", ", ALLOWED_VIDEO_EXTENSIONS),
                MIN_FILE_SIZE_BYTES / (1024 * 1024),
                MAX_FILE_SIZE_BYTES / (1024 * 1024)
        );
    }

}
