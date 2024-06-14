package com.dalhousie.servicehub.service;

import com.dalhousie.servicehub.model.ResetPasswordTokenModel;
import com.dalhousie.servicehub.repository.ResetPasswordTokenRepository;
import com.dalhousie.servicehub.service.reset_password.ResetPasswordTokenServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("LoggingSimilarMessage")
@ExtendWith(MockitoExtension.class)
public class ResetPasswordTokenServiceTest {

    private static final Logger logger = LogManager.getLogger(ResetPasswordTokenServiceTest.class);

    @Mock
    private ResetPasswordTokenRepository resetPasswordTokenRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @InjectMocks
    @Autowired
    private ResetPasswordTokenServiceImpl resetPasswordTokenService;

    @Test
    void shouldCreateResetPasswordToken_WhenUserIdDoesNotExist() {
        // Given
        logger.info("Test started: shouldCreateResetPasswordToken_WhenUserIdDoesNotExist");
        Long userId = 10L;
        ArgumentCaptor<ResetPasswordTokenModel> captor = ArgumentCaptor.forClass(ResetPasswordTokenModel.class);
        logger.info("Will return null model when finding user with specific id");
        when(resetPasswordTokenRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        resetPasswordTokenService.createResetPasswordToken(userId);

        // Then
        verify(resetPasswordTokenRepository).save(captor.capture());
        logger.info("Captured model to save in database: {}", captor.getValue());
        verify(resetPasswordTokenRepository, never()).delete(any());
        assertEquals(userId, captor.getValue().getUserId());
        logger.info("Test completed: shouldCreateResetPasswordToken_WhenUserIdDoesNotExist");
    }

    @Test
    void shouldCreateResetPasswordToken_WhenUserIdAlreadyExist() {
        // Given
        logger.info("Test started: shouldCreateResetPasswordToken_WhenUserIdAlreadyExist");
        Long userId = 10L;
        String oldToken = "oldToken";
        ArgumentCaptor<ResetPasswordTokenModel> captor = ArgumentCaptor.forClass(ResetPasswordTokenModel.class);
        ResetPasswordTokenModel dummyModel = ResetPasswordTokenModel.builder().userId(userId).token(oldToken).build();
        logger.info("Will return dummy model when finding user with specific id: {}", dummyModel);
        when(resetPasswordTokenRepository.findByUserId(userId)).thenReturn(Optional.of(dummyModel));

        // When
        resetPasswordTokenService.createResetPasswordToken(userId);

        // Then
        verify(resetPasswordTokenRepository).save(captor.capture());
        logger.info("Captured model to save in database: {}", captor.getValue());
        verify(resetPasswordTokenRepository).delete(dummyModel);
        assertEquals(userId, captor.getValue().getUserId());
        assertNotEquals(oldToken, captor.getValue().getToken());
        logger.info("Test completed: shouldCreateResetPasswordToken_WhenUserIdAlreadyExist");
    }

    @Test
    void shouldDeleteResetPasswordTokenModel() {
        // Given
        logger.info("Test started: shouldDeleteResetPasswordTokenModel");
        ResetPasswordTokenModel dummyModel = ResetPasswordTokenModel.builder().build();

        // When
        logger.info("Deleting model from database: {}", dummyModel);
        resetPasswordTokenService.deleteResetPasswordToken(dummyModel);

        // Then
        verify(resetPasswordTokenRepository).delete(dummyModel);
        logger.info("Test completed: shouldDeleteResetPasswordTokenModel");
    }

    @Test
    void shouldReturnResetPasswordTokenModel_WhenFindByUserIdIsCalled() {
        // Given
        logger.info("Test started: shouldReturnResetPasswordTokenModel_WhenFindByUserIdIsCalled");
        Long userId = 10L;
        ResetPasswordTokenModel dummyModel = ResetPasswordTokenModel.builder().userId(userId).build();
        when(resetPasswordTokenRepository.findByUserId(userId)).thenReturn(Optional.of(dummyModel));

        // When
        logger.info("Getting model from database with userId: {}", userId);
        Optional<ResetPasswordTokenModel> result = resetPasswordTokenService.findByUserId(userId);

        // Then
        verify(resetPasswordTokenRepository).findByUserId(userId);
        assertTrue(result.isPresent());
        assertEquals(result.get().getUserId(), userId);
        logger.info("Test completed: shouldReturnResetPasswordTokenModel_WhenFindByUserIdIsCalled");
    }

    @Test
    void shouldReturnFalse_WhenTokenIsInvalid_AndIsTokenValidCalled() {
        // Given
        logger.info("Test started: shouldReturnFalse_WhenTokenIsInvalid_AndIsTokenValidCalled");
        Long userId = 10L;
        ResetPasswordTokenModel dummyModel = ResetPasswordTokenModel.builder()
                .userId(userId)
                .expiryDate(Instant.now().minusMillis(1000L)) // expired before 1000 milliseconds
                .build();

        // When
        logger.info("Getting model from database with userId: {}", userId);
        boolean result = resetPasswordTokenService.isTokenValid(dummyModel);

        // Then
        assertFalse(result);
        logger.info("Test completed: shouldReturnFalse_WhenTokenIsInvalid_AndIsTokenValidCalled");
    }

    @Test
    void shouldReturnTrue_WhenTokenIsValid_AndIsTokenValidCalled() {
        // Given
        logger.info("Test started: shouldReturnTrue_WhenTokenIsValid_AndIsTokenValidCalled");
        Long userId = 10L;
        ResetPasswordTokenModel dummyModel = ResetPasswordTokenModel.builder()
                .userId(userId)
                .expiryDate(Instant.now().plusMillis(1000L)) // expiring after 1000 milliseconds
                .build();

        // When
        logger.info("Getting model from database with userId: {}", userId);
        boolean result = resetPasswordTokenService.isTokenValid(dummyModel);

        // Then
        assertTrue(result);
        logger.info("Test completed: shouldReturnTrue_WhenTokenIsValid_AndIsTokenValidCalled");
    }
}