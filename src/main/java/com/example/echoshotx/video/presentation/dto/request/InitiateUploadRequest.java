package com.example.echoshotx.video.presentation.dto.request;

import com.example.echoshotx.video.domain.entity.ProcessingType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InitiateUploadRequest {

    @NotBlank(message = "파일명은 필수입니다")
    private String fileName;

    @NotNull(message = "파일 크기는 필수입니다")
    @Min(value = 1, message = "파일 크기는 1바이트 이상이어야 합니다")
    @Max(value = 524288000, message = "파일 크기는 500MB를 초과할 수 없습니다") // todo 변경 가능
    private Long filesSizeBytes;

    @NotBlank(message = "Content-Type은 필수입니다")
    @Pattern(
            regexp = "video/(mp4|quicktime|x-msvideo|x-matroska)",
            message = "지원되지 않는 비디오 형식입니다"
    )
    private String contentType;

    @NotNull(message = "처리 타입은 필수입니다")
    private ProcessingType processingType;

}
