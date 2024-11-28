package com.dalhousie.servicehub.service.file_upload;

import com.dalhousie.servicehub.config.AwsProperties;
import com.dalhousie.servicehub.exceptions.FileUploadException;
import com.dalhousie.servicehub.response.FileUploadResponse;
import com.dalhousie.servicehub.util.ResponseBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

import static com.dalhousie.servicehub.util.ResponseBody.ResultType.SUCCESS;

@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;

    @Override
    public ResponseBody<FileUploadResponse> saveFile(Long userId, MultipartFile file) {
        if (file.isEmpty())
            throw new FileUploadException("Cannot upload file because provided file is empty");

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank())
            throw new FileUploadException("Cannot upload file because provided file name is empty");

        try {
            String fileUrl = saveAndGetPublicUrlForFile(userId, file, fileName);
            FileUploadResponse fileUploadResponse = FileUploadResponse.builder().url(fileUrl).build();
            return new ResponseBody<>(SUCCESS, fileUploadResponse, "File uploaded successfully");
        } catch (IOException exception) {
            throw new FileUploadException(exception.getMessage());
        }
    }

    /**
     * Provides publicly accessed URL for the requesting file
     *
     * @param userId   ID of the user
     * @param file     Requesting file to get public url
     * @param fileName Name of the file
     * @return String representing public URL for the file
     * @throws IOException if error occurs while reading bytes from the file
     */
    private String saveAndGetPublicUrlForFile(Long userId, MultipartFile file, String fileName) throws IOException {
        String key = "images/" + userId + "-" + fileName;
        s3Client.putObject(
                PutObjectRequest.builder().bucket(awsProperties.getBucketName()).key(key).build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        return "https://" + awsProperties.getBucketName() + ".s3." + awsProperties.getRegion() + ".amazonaws.com/" + key;
    }
}
