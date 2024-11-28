package com.dalhousie.servicehub.service;

import com.dalhousie.servicehub.config.AwsProperties;
import com.dalhousie.servicehub.exceptions.FileUploadException;
import com.dalhousie.servicehub.response.FileUploadResponse;
import com.dalhousie.servicehub.service.file_upload.FileUploadService;
import com.dalhousie.servicehub.service.file_upload.FileUploadServiceImpl;
import com.dalhousie.servicehub.util.FileHelper;
import com.dalhousie.servicehub.util.ResponseBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("LoggingSimilarMessage")
@ExtendWith(MockitoExtension.class)
public class FileUploadServiceTest {

    private static final Logger logger = LogManager.getLogger(FileUploadServiceTest.class);
    private FileUploadService fileUploadService;

    @Mock
    private FileHelper fileHelper;

    @Mock
    private S3Client s3Client;

    @Mock
    private AwsProperties awsProperties;

    @BeforeEach
    public void setUp() {
        fileUploadService = new FileUploadServiceImpl(s3Client, awsProperties);
    }

    @Test
    public void shouldThrowFileUploadException_whenFileIsEmpty() {
        // Given
        logger.info("Test started: throwing FileUploadException when file is empty");
        Long userId = 1L;
        MultipartFile file = mock(MultipartFile.class);

        logger.info("Will return true when it checks for if file is empty");
        when(file.isEmpty()).thenReturn(true);

        // When
        FileUploadException fileUploadException = assertThrows(FileUploadException.class,
                () -> fileUploadService.saveFile(userId, file));

        // Then
        assertEquals("Cannot upload file because provided file is empty", fileUploadException.getMessage());
        logger.info("Test completed: throwing FileUploadException when file is empty");
    }

    @Test
    public void shouldThrowFileUploadException_whenFileNameIsNullOrBlank() {
        // Given
        logger.info("Test started: throwing FileUploadException when file name is null or blank");
        Long userId = 1L;
        MultipartFile file = mock(MultipartFile.class);

        logger.info("Will return false when it checks for if file is empty");
        when(file.isEmpty()).thenReturn(false);

        // When
        logger.info("Will return null when trying to get file name from file");
        when(file.getOriginalFilename()).thenReturn(null);
        FileUploadException fileUploadException = assertThrows(FileUploadException.class,
                () -> fileUploadService.saveFile(userId, file));

        // Then
        assertEquals("Cannot upload file because provided file name is empty", fileUploadException.getMessage());

        // When
        logger.info("Will return empty string when trying to get file name from file");
        when(file.getOriginalFilename()).thenReturn("");
        FileUploadException fileUploadException2 = assertThrows(FileUploadException.class,
                () -> fileUploadService.saveFile(userId, file));

        // Then
        assertEquals("Cannot upload file because provided file name is empty", fileUploadException2.getMessage());
        logger.info("Test completed: throwing FileUploadException when file name is null or blank");
    }

//    @Test
//    public void shouldThrowFileUploadException_whenTryingToGetBytesFromFile() throws IOException {
//        // Given
//        logger.info("Test started: throwing FileUploadException when trying to get bytes from file");
//        Long userId = 1L;
//        String fileName = "abcd.png";
//        MultipartFile file = mock(MultipartFile.class);
//
//        logger.info("Will return false when it checks for if file is empty");
//        when(file.isEmpty()).thenReturn(false);
//
//        logger.info("Will return {} when trying to get file name from file", fileName);
//        when(file.getOriginalFilename()).thenReturn(fileName);
//        doThrow(IOException.class).when(file).getBytes();
//
//        // When & Then
//        assertThrows(FileUploadException.class, () -> fileUploadService.saveFile(userId, file));
//        logger.info("Test completed: throwing FileUploadException when trying to get bytes from file");
//    }

//    @Test
//    public void shouldSaveFileAndReturnResponse_whenFileIsValid() throws IOException {
//        // Given
//        logger.info("Test started: Save file when file is valid");
//        Long userId = 1L;
//        String fileName = "abcd.png";
//        AtomicReference<ResponseBody<FileUploadResponse>> response = new AtomicReference<>();
//        MultipartFile file = mock(MultipartFile.class);
//
//        logger.info("Will return false when it checks for if file is empty");
//        when(file.isEmpty()).thenReturn(false);
//
//        logger.info("Will return {} when trying to get file name from file", fileName);
//        when(file.getOriginalFilename()).thenReturn(fileName);
//        when(file.getBytes()).thenReturn(new byte[0]);
//
//        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
//        mockRequest.setContextPath("/");
//        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
//        RequestContextHolder.setRequestAttributes(attrs);
//
//        // When
//        assertDoesNotThrow(() -> response.set(fileUploadService.saveFile(userId, file)));
//
//        // Then
//        assertNotNull(response.get());
//        assertFalse(response.get().data().getUrl().isEmpty());
//        logger.info("Test completed: Save file when file is valid");
//    }
//
//    @Test
//    public void shouldSaveFileAndReturnResponse_whenFileIsValid_andFileHasNoExtension() throws IOException {
//        // Given
//        logger.info("Test started: Save file when file is valid");
//        Long userId = 1L;
//        String fileName = "abcd";
//        AtomicReference<ResponseBody<FileUploadResponse>> response = new AtomicReference<>();
//        MultipartFile file = mock(MultipartFile.class);
//
//        logger.info("Will return false when it checks for if file is empty");
//        when(file.isEmpty()).thenReturn(false);
//
//        logger.info("Will return {} when trying to get file name from file", fileName);
//        when(file.getOriginalFilename()).thenReturn(fileName);
//        when(file.getBytes()).thenReturn(new byte[0]);
//
//        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
//        mockRequest.setContextPath("/");
//        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
//        RequestContextHolder.setRequestAttributes(attrs);
//
//        // When
//        assertDoesNotThrow(() -> response.set(fileUploadService.saveFile(userId, file)));
//
//        // Then
//        assertNotNull(response.get());
//        assertFalse(response.get().data().getUrl().isEmpty());
//        logger.info("Test completed: Save file when file is valid");
//    }
}
