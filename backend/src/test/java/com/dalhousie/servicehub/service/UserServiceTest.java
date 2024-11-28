package com.dalhousie.servicehub.service;

import com.dalhousie.servicehub.config.AwsProperties;
import com.dalhousie.servicehub.exceptions.InvalidTokenException;
import com.dalhousie.servicehub.exceptions.UserAlreadyExistException;
import com.dalhousie.servicehub.exceptions.UserNotFoundException;
import com.dalhousie.servicehub.model.ResetPasswordTokenModel;
import com.dalhousie.servicehub.model.UserModel;
import com.dalhousie.servicehub.repository.UserRepository;
import com.dalhousie.servicehub.request.AuthenticationRequest;
import com.dalhousie.servicehub.request.RegisterRequest;
import com.dalhousie.servicehub.request.ResetPasswordRequest;
import com.dalhousie.servicehub.response.AuthenticationResponse;
import com.dalhousie.servicehub.service.blacklist_token.BlackListTokenService;
import com.dalhousie.servicehub.service.jwt.JwtService;
import com.dalhousie.servicehub.service.reset_password.ResetPasswordTokenService;
import com.dalhousie.servicehub.service.user.UserServiceImpl;
import com.dalhousie.servicehub.util.EmailSender;
import com.dalhousie.servicehub.util.ResponseBody;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.services.sns.SnsClient;

import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("LoggingSimilarMessage")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Logger logger = LogManager.getLogger(UserServiceTest.class);

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ResetPasswordTokenService resetPasswordTokenService;

    @Mock
    private BlackListTokenService blackListTokenService;

    @Mock
    private EmailSender emailSender;

    @Mock
    private SnsClient snsClient;

    @Mock
    private AwsProperties awsProperties;

    private UserServiceImpl userService;

    private UserModel userModel;
    private AuthenticationRequest authRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        userModel = UserModel.builder()
                .name("Jems Patel")
                .email("jems007patel@gmail.com")
                .password("Jems@007")
                .phone("7825258252")
                .address("1881 Brunswick Street")
                .image("image.jpg")
                .build();
        authRequest = new AuthenticationRequest("jems007patel@gmail.com", "Jems@007");
        registerRequest = RegisterRequest.builder()
                .name("Jems Patel")
                .email("jems007patel@gmail.com")
                .password("Jems@007")
                .phone("7825258252")
                .address("1881 Brunswick Street")
                .image("image.jpg")
                .build();
        userService = new UserServiceImpl(3000,
                userRepository,
                passwordEncoder,
                jwtService,
                emailSender,
                resetPasswordTokenService,
                authenticationManager,
                blackListTokenService,
                snsClient,
                awsProperties);
    }

    @Test
    void shouldNotRegister_WhenUserIsAlreadyRegistered() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userModel));

        // When
        UserAlreadyExistException exception = assertThrows(UserAlreadyExistException.class, () ->
                userService.registerUser(registerRequest)
        );

        // Then
        assertEquals(exception.getMessage(), "User with this email already exists.");
        verify(userRepository, never()).save(any());
    }

//    @Test
//    @DisplayName("Registration Successfully Completed")
//    void UserRegisterHandlerTest() {
//        logger.info("Starting test: Registration Successfully Completed");
//        when(userRepository.save(any(UserModel.class))).thenReturn(userModel);
//        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");
//
//        logger.info("Registering user with details: {}", userModel);
//        ResponseBody<AuthenticationResponse> response = userService.registerUser(registerRequest);
//
//        logger.info("Received response: {}", response);
//        assertNotNull(response);
//        assertEquals("jwt-token", response.data().getToken());
//
//        verify(userRepository, times(1)).save(any(UserModel.class));
//        verify(jwtService, times(1)).generateToken(any(UserDetails.class));
//
//        logger.info("Test completed: Registration Successfully Completed");
//    }

    @Test
    @DisplayName("Successfully login")
    void userLoginHandlerTest() {
        logger.info("Starting test: Successfully login");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userModel));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("jwt-token");

        logger.info("Authenticating user with request: {}", authRequest);
        ResponseBody<AuthenticationResponse> response = userService.authenticateUser(authRequest);

        logger.info("Received response: {}", response);
        assertNotNull(response);
        assertEquals("jwt-token", response.data().getToken());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(2)).findByEmail(anyString());
        verify(jwtService, times(1)).generateToken(any(UserDetails.class));
        logger.info("Test completed: Successfully login");

    }

    @Test
    @DisplayName("Login with Invalid Credentials")
    void userLoginHandlerTest_WithInvalidCredentials() {
        logger.info("Starting test: Login with Invalid Credentials");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Bad credentials"));
        when(userRepository.findByEmail("jems007patel@gmail.com")).thenReturn(Optional.of(userModel));

        AuthenticationRequest invalidAuthRequest = new AuthenticationRequest("jems007patel@gmail.com", "wrongpassword");

        logger.info("Authenticating user with invalid request: {}", invalidAuthRequest);
        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.authenticateUser(invalidAuthRequest));

        logger.info("Exception thrown: {}", exception.getMessage());
        assertEquals("Bad credentials", exception.getMessage());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(0)).generateToken(any(UserDetails.class));

        logger.info("Test completed: Login with Invalid Credentials");
    }

    @Test
    @DisplayName("Login with Non-existing User")
    void userLoginHandlerTest_WithNonExistingUser() {
        logger.info("Starting test: Login with Non-existing User");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        logger.info("Authenticating user with request: {}", authRequest);
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> userService.authenticateUser(authRequest));

        logger.info("Exception thrown: {}", exception.getMessage());
        assertEquals("User not found with email: jems007patel@gmail.com", exception.getMessage());
        assertEquals("User not found with email: jems007patel@gmail.com", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, times(1)).findByEmail(anyString());
        verify(jwtService, never()).generateToken(any(UserDetails.class));

        logger.info("Test completed: Login with Non-existing User");
    }

    @Test
    void shouldThrowException_WhenUserNotRegistered_AndResetPasswordCalled() {
        // Given
        logger.info("Starting test: Reset password without registering");
        String email = "dummy@gmail.com";
        String password = "12345678";
        String token = "someRandomToken";
        logger.info("Will return no user when trying to get user by email");
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () ->
                userService.resetPassword(new ResetPasswordRequest(email, password, token))
        );

        // Then
        logger.info("Caught user not found exception for {}", email);
        assertEquals(exception.getMessage(), "User not found with email: " + email);
        verify(passwordEncoder, never()).encode(password);
        verify(userRepository, never()).updatePassword(email, password);
        verify(resetPasswordTokenService, never()).deleteResetPasswordToken(any());
        logger.info("Test completed: Reset password without registering");
    }

    @Test
    void shouldThrowException_WhenUserNotRequestedResetPassword_AndResetPasswordCalled() {
        // Given
        logger.info("Starting test: Reset password without requesting for reset password");
        Long userId = 10L;
        String email = "dummy@gmail.com";
        String password = "12345678";
        String token = "someRandomToken";
        UserModel dummyUserModel = UserModel.builder().id(userId).build();

        logger.info("Will return dummy user model when finding by email: {}", dummyUserModel);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dummyUserModel));

        logger.info("Will return null user when trying to get reset password model from user id.");
        when(resetPasswordTokenService.findByUserId(dummyUserModel.getId())).thenReturn(Optional.empty());

        // When
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () ->
                userService.resetPassword(new ResetPasswordRequest(email, password, token))
        );

        // Then
        logger.info("Caught user not found exception while reset password without requesting for reset password for {}", email);
        assertEquals(exception.getMessage(), "User did not initiated reset password request.");
        verify(passwordEncoder, never()).encode(password);
        verify(userRepository, never()).updatePassword(email, password);
        verify(resetPasswordTokenService, never()).deleteResetPasswordToken(any());
        logger.info("Test completed: Reset password without requesting for reset password");
    }

    @Test
    void shouldThrowException_WhenInvalidTokenPassed_AndResetPasswordCalled() {
        // Given
        logger.info("Starting test: Reset password with invalid token");
        Long userId = 10L;
        String email = "dummy@gmail.com";
        String password = "12345678";
        String token = "someRandomToken";
        UserModel dummyUserModel = UserModel.builder().id(userId).build();
        ResetPasswordTokenModel resetPasswordTokenModel = ResetPasswordTokenModel.builder().token("differentToken").build();

        logger.info("Will return dummy user model when finding by email: {}", dummyUserModel);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dummyUserModel));

        logger.info("Will return dummy reset password model when finding by user id: {}", resetPasswordTokenModel);
        when(resetPasswordTokenService.findByUserId(dummyUserModel.getId())).thenReturn(Optional.of(resetPasswordTokenModel));

        // When
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () ->
                userService.resetPassword(new ResetPasswordRequest(email, password, token))
        );

        // Then
        logger.info("Caught invalid token exception for invalid token by email: {}", email);
        assertEquals(exception.getMessage(), "Failed to authenticate token. Please re-request to reset password.");
        verify(passwordEncoder, never()).encode(password);
        verify(userRepository, never()).updatePassword(email, password);
        verify(resetPasswordTokenService, never()).deleteResetPasswordToken(any());
        logger.info("Test completed: Reset password with invalid token");
    }

    @Test
    void shouldThrowException_WhenExpiredTokenPassed_AndResetPasswordCalled() {
        // Given
        logger.info("Starting test: Reset password with expired token");
        Long userId = 10L;
        String email = "dummy@gmail.com";
        String password = "12345678";
        String token = "someRandomToken";
        UserModel dummyUserModel = UserModel.builder().id(userId).build();
        ResetPasswordTokenModel resetPasswordTokenModel = ResetPasswordTokenModel.builder().token(token).build();

        logger.info("Will return dummy user model when finding by email: {}", dummyUserModel);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dummyUserModel));

        logger.info("Will return dummy reset password model when finding by user id: {}", resetPasswordTokenModel);
        when(resetPasswordTokenService.findByUserId(dummyUserModel.getId())).thenReturn(Optional.of(resetPasswordTokenModel));

        logger.info("Will return false when is token valid function called for {}", resetPasswordTokenModel);
        when(resetPasswordTokenService.isTokenValid(resetPasswordTokenModel)).thenReturn(false);

        // When
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () ->
                userService.resetPassword(new ResetPasswordRequest(email, password, token))
        );

        // Then
        logger.info("Caught invalid token exception for expired token by email: {}", email);
        assertEquals(exception.getMessage(), "Token expired. Please re-request to reset password");
        verify(passwordEncoder, never()).encode(password);
        verify(userRepository, never()).updatePassword(email, password);
        verify(resetPasswordTokenService, never()).deleteResetPasswordToken(any());
        logger.info("Test completed: Reset password with expired token");
    }

    @Test
    void shouldUpdatePassword_WhenUserIsRegistered_AndTokenIsValid() {
        // Given
        logger.info("Starting test: Reset password with user registered and valid token");
        Long userId = 10L;
        String email = "dummy@gmail.com";
        String password = "12345678";
        String token = "someRandomToken";
        String encodedPassword = "axbyhugjdigjalnge";
        UserModel dummyUserModel = UserModel.builder().id(userId).build();
        ResetPasswordTokenModel resetPasswordTokenModel = ResetPasswordTokenModel.builder().token(token).build();

        logger.info("Will return dummy user model when finding by email: {}", dummyUserModel);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(dummyUserModel));

        logger.info("Will return dummy reset password model when finding by user id: {}", resetPasswordTokenModel);
        when(resetPasswordTokenService.findByUserId(dummyUserModel.getId())).thenReturn(Optional.of(resetPasswordTokenModel));

        logger.info("Will return true when is token valid function called for {}", resetPasswordTokenModel);
        when(resetPasswordTokenService.isTokenValid(resetPasswordTokenModel)).thenReturn(true);

        logger.info("Will return encoded password ({}) when trying to encode password", encodedPassword);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);

        // When
        userService.resetPassword(new ResetPasswordRequest(email, password, token));

        // Then
        verify(passwordEncoder).encode(password);
        verify(userRepository).updatePassword(email, encodedPassword);
        verify(resetPasswordTokenService).deleteResetPasswordToken(resetPasswordTokenModel);
        logger.info("Test completed: Reset password with user registered and valid token");
    }

    @Test
    @DisplayName("Throw exception when user not found")
    void forgotPassword_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.forgotPassword("jems007patel@gmail.com", mock(HttpServletRequest.class)));

        verify(userRepository, times(1)).findByEmail(anyString());
    }

    @Test
    @DisplayName("Throw some exception occurred while sending mail")
    void forgotPassword_shouldThrowException_whenSomeExceptionOccurred() throws MessagingException, UnsupportedEncodingException {
        // Given
        String email = "jems007patel@gmail.com";
        String resetUrl = "http://127.0.0.1:8080/reset-password";
        ResetPasswordTokenModel resetPasswordTokenModel = ResetPasswordTokenModel.builder().token("someToken").build();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userModel));
        when(resetPasswordTokenService.createResetPasswordToken(userModel.getId())).thenReturn(resetPasswordTokenModel);
        doThrow(new RuntimeException("Exception occurred while sending mail"))
                .when(emailSender).sendEmail(any(), any(), any());
        when(request.getRequestURL()).thenReturn(new StringBuffer(resetUrl));
        when(request.getServletPath()).thenReturn("");

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.forgotPassword(email, request)
        );

        // Then
        verify(userRepository, times(1)).findByEmail(anyString());
        assertEquals(exception.getMessage(), "Exception occurred while sending mail");
    }

    @Test
    @DisplayName("Should send mail when user is registered")
    void forgotPassword_shouldSendEmail_whenUserIsRegistered() {
        // Given
        String email = "jems007patel@gmail.com";
        String resetUrl = "http://127.0.0.1:8080/reset-password";
        ResetPasswordTokenModel resetPasswordTokenModel = ResetPasswordTokenModel.builder().token("someToken").build();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userModel));
        when(resetPasswordTokenService.createResetPasswordToken(userModel.getId())).thenReturn(resetPasswordTokenModel);
        when(request.getRequestURL()).thenReturn(new StringBuffer(resetUrl));
        when(request.getServletPath()).thenReturn("");

        // When & Then
        assertDoesNotThrow(() -> userService.forgotPassword(email, request));
    }

    @Test
    void shouldThrowException_whenPortCannotBeChanged() {
        // Given
        HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
        when(mockHttpServletRequest.getRequestURL()).thenReturn(new StringBuffer());
        when(mockHttpServletRequest.getServletPath()).thenReturn("");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userModel));

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.forgotPassword("", mockHttpServletRequest)
        );

        // Then
        assertEquals(exception.getMessage(), "Failed to change URL port");
    }

    @Test
    @DisplayName("Sign out user and blacklist token")
    void signOut() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String token = "sampleToken";

        logger.info("Getting token from header");
        lenient().when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        // When
        logger.info("Calling signOut method");
        userService.signOut(token);

        // Then
        verify(blackListTokenService, times(1)).addBlackListToken(token);
        logger.info("Test completed: Sign out user and blacklist token.");
    }
}
