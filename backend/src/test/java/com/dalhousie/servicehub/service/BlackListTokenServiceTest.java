package com.dalhousie.servicehub.service;

import com.dalhousie.servicehub.controller.AuthController;
import com.dalhousie.servicehub.exceptions.BlackListTokenAlreadyExistsException;
import com.dalhousie.servicehub.model.BlackListTokenModel;
import com.dalhousie.servicehub.repository.BlackListRepository;
import com.dalhousie.servicehub.service.blacklist_token.BlackListTokenServiceImpl;
import com.dalhousie.servicehub.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlackListTokenServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private BlackListRepository blackListRepository;

    @InjectMocks
    private AuthController authController;

    @InjectMocks
    private BlackListTokenServiceImpl blackListTokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should sign out successfully")
    void signOutHandler_ShouldSignOutSuccessfully() throws Exception {
        // Given
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // When
        ResponseEntity<Object> response = authController.signOutHandler(request);

        // Then
        verify(userService, times(1)).signOut(token);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User signed out successfully.", response.getBody());
    }

    @Test
    @DisplayName("Should return bad request when token is already blacklisted")
    void signOutHandler_ShouldReturnBadRequest_WhenTokenIsAlreadyBlacklisted() {
        // Given
        String token = "blacklistedToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        doThrow(new BlackListTokenAlreadyExistsException("Token is already blacklisted")).when(userService).signOut(token);

        // When
        ResponseEntity<Object> response = authController.signOutHandler(request);

        // Then
        verify(userService, times(1)).signOut(token);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Token is already blacklisted", response.getBody());
    }

    @Test
    @DisplayName("Should return bad request for unexpected errors during sign-out")
    void signOutHandler_ShouldReturnBadRequest_ForUnexpectedErrors() {
        // Given
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        doThrow(new RuntimeException("Unexpected error")).when(userService).signOut(token);

        // When
        ResponseEntity<Object> response = authController.signOutHandler(request);

        // Then
        verify(userService, times(1)).signOut(token);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Unexpected error", response.getBody());
    }
    @Test
    @DisplayName("Should add token to blacklist successfully")
    void addBlackListToken_ShouldAddTokenSuccessfully() {
        // Given
        String token = "validToken";
        when(blackListRepository.findByToken(token)).thenReturn(Optional.empty());

        // When
        blackListTokenService.addBlackListToken(token);

        // Then
        verify(blackListRepository, times(1)).save(any(BlackListTokenModel.class));
    }

    @Test
    @DisplayName("Should throw exception when token already exists in blacklist")
    void addBlackListToken_ShouldThrowException_WhenTokenAlreadyExists() {
        // Given
        String token = "existingToken";
        when(blackListRepository.findByToken(token)).thenReturn(Optional.of(new BlackListTokenModel()));

        // When & Then
        BlackListTokenAlreadyExistsException exception = assertThrows(BlackListTokenAlreadyExistsException.class, () -> {
            blackListTokenService.addBlackListToken(token);
        });

        assertEquals("BlackList Token already exists", exception.getMessage());
        verify(blackListRepository, times(0)).save(any(BlackListTokenModel.class));
    }

    @Test
    @DisplayName("Should return true when token exists in blacklist")
    void doesBlackListTokenExists_ShouldReturnTrue_WhenTokenExists() {
        // Given
        String token = "existingToken";
        when(blackListRepository.findByToken(token)).thenReturn(Optional.of(new BlackListTokenModel()));

        // When
        boolean exists = blackListTokenService.doesBlackListTokenExists(token);

        // Then
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when token does not exist in blacklist")
    void doesBlackListTokenExists_ShouldReturnFalse_WhenTokenDoesNotExist() {
        // Given
        String token = "nonExistingToken";
        when(blackListRepository.findByToken(token)).thenReturn(Optional.empty());

        // When
        boolean exists = blackListTokenService.doesBlackListTokenExists(token);

        // Then
        assertFalse(exists);
    }

}