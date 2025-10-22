package com.example.echoshotx.infrastructure.aws.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.HttpMethod;

import com.example.echoshotx.infrastructure.aws.validator.S3Validator;
import com.example.echoshotx.infrastructure.exception.object.domain.S3Handler;
import com.example.echoshotx.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String staticRegion;

    private final AmazonS3Client amazonS3Client;

    private static final String S3_AWS_STATIC_PATH = "https://%s.s3.%s.amazonaws.com/";
    private static final String LOCAL_FILE_PATH = "src/main/resources/dump/";

    // Pre-signed URL 만료 시간 설정
    private static final int UPLOAD_URL_EXPIRATION = 900; // 15분
    private static final int STREAMING_URL_EXPIRATION = 3600; // 1시간
    private static final int DOWNLOAD_URL_EXPIRATION = 1800;  // 30분
    private static final int THUMBNAIL_URL_EXPIRATION = 86400; // 24시간

    public URL generateUploadUrl(String s3Key, String contentType, long contentLength) {
        S3Validator.validateUploadSize(contentLength);
        S3Validator.validateVideoContentType(contentType);

        // 만료 시간 설정
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + UPLOAD_URL_EXPIRATION * 1000L);

        // Presigned URL 생성
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration)
                .withContentType(contentType);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(contentLength);

        return amazonS3Client.generatePresignedUrl(request);
    }

    public void deleteFile(String s3Key) {
        amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, s3Key));
    }

    public String uploadVideo(MultipartFile video, String filePath) {
        validateVideoSize(video);
        validateVideoFileExtension(video.getOriginalFilename());
        String fileName = createVideoFileName(video);
        String s3Key = filePath + fileName;
        try {
            InputStream inputStream = video.getInputStream();
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(video.getSize());
            metadata.setContentType(video.getContentType());

            amazonS3Client.putObject(new PutObjectRequest(bucket, s3Key, inputStream, metadata).withCannedAcl(
                    CannedAccessControlList.Private));
        } catch (IOException e) {
            throw new S3Handler(ErrorStatus.FILE_UPLOAD_FAILED);
        }

        return fileName;
    }

    public String uploadImage(MultipartFile image, String filePath) {
        validateImageFileExtension(image.getOriginalFilename());
        String fileName = createImageFileName(image);
        String s3Key = filePath + fileName;
        try {
            File uploadFile = uploadLocalFile(image, fileName, filePath).orElseThrow(
                    () -> new S3Handler(ErrorStatus.FILE_INVALID_EXTENSION)
            );
            amazonS3Client.putObject(new PutObjectRequest(bucket, s3Key, uploadFile).withCannedAcl(
                    CannedAccessControlList.PublicRead));
            removeNewFile(uploadFile);
        } catch (IOException e) {
            throw new S3Handler(ErrorStatus.FILE_UPLOAD_FAILED);
        }

        return fileName;
    }

    public String generateFileUrl(String s3Key) {
        return String.format(S3_AWS_STATIC_PATH, bucket, staticRegion) + s3Key;
    }

    private String createImageFileName(MultipartFile multipartFile) {
        return UUID.randomUUID().toString().substring(0, 10) + multipartFile.getOriginalFilename();
    }

    private String createVideoFileName(MultipartFile multipartFile) {
        return UUID.randomUUID().toString().substring(0, 10) + multipartFile.getOriginalFilename();
    }

    private void validateVideoFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new S3Handler(ErrorStatus.FILE_EXTENSION_NOT_FOUND);
        }

        String extention = filename.substring(lastDotIndex + 1).toLowerCase();
        List<String> allowedExtentionList = Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "mkv");

        if (!allowedExtentionList.contains(extention)) {
            throw new S3Handler(ErrorStatus.FILE_INVALID_EXTENSION);
        }
    }

    private void validateImageFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new S3Handler(ErrorStatus.FILE_EXTENSION_NOT_FOUND);
        }

        String extention = fileName.substring(lastDotIndex + 1).toLowerCase();
        List<String> allowedExtentionList = Arrays.asList("jpg", "jpeg", "png", "gif");

        if (!allowedExtentionList.contains(extention)) {
            throw new S3Handler(ErrorStatus.FILE_INVALID_EXTENSION);
        }
    }

    private void validateVideoSize(MultipartFile video) {
        long maxSizeBytes = 500 * 1024 * 1024; // 500MB
        if (video.getSize() > maxSizeBytes) {
            throw new S3Handler(ErrorStatus.FILE_UPLOAD_FAILED);
        }
    }


    private Optional<File> uploadLocalFile(MultipartFile multipartFile, String fileName, String filePath) throws IOException {
        String localPathName = LOCAL_FILE_PATH + filePath + fileName;
        File convertFile = new File(localPathName);

        createParentDir(convertFile);

        if (convertFile.createNewFile()) {
            try (FileOutputStream fos = new FileOutputStream(convertFile)) { // FileOutputStream 데이터를 파일에 바이트 스트림으로 저장하기 위함
                fos.write(multipartFile.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }

    private static void createParentDir(File convertFile) {
        // 상위 디렉토리 생성
        File parentDir = convertFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // 모든 중간 디렉토리 생성
        }
    }

    private void removeNewFile(File file) {
        if (file.delete()) {
            log.info("File delete success");
            return;
        }
        log.info("File delete fail");
    }

    /**
     * 영상 스트리밍을 위한 Pre-signed URL 생성
     * @param s3Key S3 객체 키
     * @return 스트리밍 가능한 Pre-signed URL
     */
    public String generateStreamingUrl(String s3Key) {
        try {
            Date expiration = new Date();
            expiration.setTime(expiration.getTime() + STREAMING_URL_EXPIRATION * 1000L);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, s3Key)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            
            URL url = amazonS3Client.generatePresignedUrl(request);
            log.info("Generated streaming URL for key: {}, expires: {}", s3Key, expiration);
            
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate streaming URL for key: {}", s3Key, e);
            throw new S3Handler(ErrorStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 영상 다운로드를 위한 Pre-signed URL 생성
     * @param s3Key S3 객체 키
     * @return 다운로드 가능한 Pre-signed URL
     */
    public String generateDownloadUrl(String s3Key) {
        try {
            Date expiration = new Date();
            expiration.setTime(expiration.getTime() + DOWNLOAD_URL_EXPIRATION * 1000L);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, s3Key)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            
            URL url = amazonS3Client.generatePresignedUrl(request);
            log.info("Generated download URL for key: {}, expires: {}", s3Key, expiration);
            
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate download URL for key: {}", s3Key, e);
            throw new S3Handler(ErrorStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 썸네일 이미지를 위한 Pre-signed URL 생성
     * @param s3Key S3 객체 키
     * @return 썸네일 표시용 Pre-signed URL
     */
    public String generateThumbnailUrl(String s3Key) {
        try {
            Date expiration = new Date();
            expiration.setTime(expiration.getTime() + THUMBNAIL_URL_EXPIRATION * 1000L);
            
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, s3Key)
                    .withMethod(HttpMethod.GET)
                    .withExpiration(expiration);
            
            URL url = amazonS3Client.generatePresignedUrl(request);
            
            return url.toString();
        } catch (Exception e) {
            log.error("Failed to generate thumbnail URL for key: {}", s3Key, e);
            throw new S3Handler(ErrorStatus.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * S3 객체 존재 여부 확인
     * @param s3Key S3 객체 키
     * @return 존재 여부
     */
    public boolean doesObjectExist(String s3Key) {
        try {
            return amazonS3Client.doesObjectExist(bucket, s3Key);
        } catch (Exception e) {
            log.error("Failed to check object existence for key: {}", s3Key, e);
            return false;
        }
    }
}
