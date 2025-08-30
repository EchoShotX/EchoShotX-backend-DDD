package com.example.echoshotx.domain.video.validator;

import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Video 도메인의 입력 검증을 담당하는 Validator
 * 
 * 📋 책임 분리 원칙:
 * ✅ VideoValidator: 단순 입력 검증 (파일 형식, 크기 제한 등)
 * ✅ Video Entity: 핵심 도메인 규칙 (상태 전환, 비즈니스 로직 등)
 * 
 * 주요 책임 (입력 검증만):
 * 1. 영상 파일 형식 검증 (확장자)
 * 2. 파일 크기 제한 검증 (최소/최대)
 * 3. 파일명 형식 검증
 * 4. 기술적 제약사항 검증
 * 
 * ❌ 하지 않는 것:
 * - 도메인 비즈니스 규칙 (상태 전환 등)
 * - 영상 처리 관련 로직
 * - 사용자 권한 검증
 */
@Component
public class VideoValidator {

    // 파일 형식 및 크기 제한 상수
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
            "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm", "m4v"
    );
    
    private static final long MAX_FILE_SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    private static final long MIN_FILE_SIZE_BYTES = 1024 * 1024; // 1MB

    /**
     * 영상 파일의 입력 유효성을 검증합니다 (단순 입력 검증)
     * 도메인 규칙이 아닌 기술적/형식적 검증만 수행
     * 
     * @param file 업로드할 영상 파일
     * @throws IllegalArgumentException 파일이 유효하지 않은 경우
     */
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

    /**
     * 파일 확장자를 검증합니다 (입력 형식 검증)
     * 
     * @param filename 파일명
     * @throws IllegalArgumentException 허용되지 않는 확장자인 경우
     */
    public void validateFileExtension(String filename) {
        String extension = getFileExtension(filename);
        if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_UNSUPPORTED_FORMAT);
        }
    }

    /**
     * 파일 크기를 검증합니다 (입력 형식 검증)
     * 
     * @param fileSizeBytes 파일 크기 (바이트)
     * @throws IllegalArgumentException 파일 크기가 제한을 벗어난 경우
     */
    public void validateFileSize(long fileSizeBytes) {
        if (fileSizeBytes < MIN_FILE_SIZE_BYTES) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_FILE_TOO_SMALL);
        }

        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_FILE_TOO_LARGE);
        }
    }

    /**
     * 파일명에서 확장자를 추출합니다
     * 
     * @param filename 파일명
     * @return 파일 확장자 (소문자)
     * @throws IllegalArgumentException 파일 확장자를 찾을 수 없는 경우
     */
    public String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new VideoHandler(VideoErrorStatus.VIDEO_EXTENSION_NOT_FOUND);
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 파일이 허용된 형식인지 확인합니다
     * 
     * @param filename 파일명
     * @return 허용된 형식인지 여부
     */
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

    /**
     * 파일 크기가 허용 범위 내인지 확인합니다
     * 
     * @param fileSizeBytes 파일 크기 (바이트)
     * @return 허용 범위 내인지 여부
     */
    public boolean isValidFileSize(long fileSizeBytes) {
        return fileSizeBytes >= MIN_FILE_SIZE_BYTES && fileSizeBytes <= MAX_FILE_SIZE_BYTES;
    }

    /**
     * 파일 검증 규칙을 반환합니다
     * 
     * @return 검증 규칙 문자열
     */
    public String getFileValidationRules() {
        return String.format(
                "파일 형식: %s, 최소 크기: %d MB, 최대 크기: %d MB",
                String.join(", ", ALLOWED_VIDEO_EXTENSIONS),
                MIN_FILE_SIZE_BYTES / (1024 * 1024),
                MAX_FILE_SIZE_BYTES / (1024 * 1024)
        );
    }

    /**
     * 허용된 파일 확장자 목록을 반환합니다
     * 
     * @return 허용된 확장자 목록
     */
    public List<String> getAllowedExtensions() {
        return List.copyOf(ALLOWED_VIDEO_EXTENSIONS);
    }

    /**
     * 최대 파일 크기를 반환합니다 (MB 단위)
     * 
     * @return 최대 파일 크기 (MB)
     */
    public long getMaxFileSizeMB() {
        return MAX_FILE_SIZE_BYTES / (1024 * 1024);
    }

    /**
     * 최소 파일 크기를 반환합니다 (MB 단위)
     * 
     * @return 최소 파일 크기 (MB)
     */
    public long getMinFileSizeMB() {
        return MIN_FILE_SIZE_BYTES / (1024 * 1024);
    }
}
