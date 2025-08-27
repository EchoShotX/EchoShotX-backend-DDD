package com.example.demo.infrastructure.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

import com.example.demo.infrastructure.exception.object.domain.S3Handler;
import com.example.demo.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public void deleteFile(String s3Key) {
        amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, s3Key));
    }

    public String uploadImage(MultipartFile image, String filePath) {
        validateFileExtension(image.getOriginalFilename());
        String fileName = createFileName(image);
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

    private String createFileName(MultipartFile multipartFile) {
        return UUID.randomUUID().toString().substring(0, 10) + multipartFile.getOriginalFilename();
    }

    private void validateFileExtension(String fileName) {
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
}
