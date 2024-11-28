package com.dalhousie.servicehub.service.user;

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
import com.dalhousie.servicehub.util.Constants;
import com.dalhousie.servicehub.util.EmailSender;
import com.dalhousie.servicehub.util.ResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.Objects;

import static com.dalhousie.servicehub.util.ResponseBody.ResultType.SUCCESS;

@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final int frontendPort;
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSender emailSender;
    private final ResetPasswordTokenService resetPasswordTokenService;
    private final AuthenticationManager authenticationManager;
    private final BlackListTokenService blackListTokenService;
    private final SnsClient snsClient;
    private final AwsProperties awsProperties;

    @Override
    public ResponseBody<AuthenticationResponse> registerUser(RegisterRequest registerRequest) {
        if (repository.findByEmail(registerRequest.getEmail()).isPresent())
            throw new UserAlreadyExistException("User with this email already exists.");
        var userModel = UserModel.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .phone(registerRequest.getPhone())
                .address(registerRequest.getAddress())
                .image(registerRequest.getImage())
                .build();
        repository.save(userModel);
        subscribeToEventNotifications(registerRequest.getEmail());
        var jwtToken = jwtService.generateToken(userModel);
        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
        return new ResponseBody<>(SUCCESS, authenticationResponse, "User successfully registered");
    }

    @Override
    public ResponseBody<AuthenticationResponse> authenticateUser(AuthenticationRequest authenticationRequest) {
        if (repository.findByEmail(authenticationRequest.getEmail()).isEmpty())
            throw new UserNotFoundException("User not found with email: " + authenticationRequest.getEmail());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authenticationRequest.getEmail(),
                        authenticationRequest.getPassword()
                )
        );
        var user = repository.findByEmail(authenticationRequest.getEmail()).get();
        var jwtToken = jwtService.generateToken(user);
        AuthenticationResponse authenticationResponse = AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
        return new ResponseBody<>(SUCCESS, authenticationResponse, "User authenticated successfully");
    }

    @Override
    public ResponseBody<String> resetPassword(ResetPasswordRequest resetPasswordRequest) {
        UserModel user = repository.findByEmail(resetPasswordRequest.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + resetPasswordRequest.getEmail()));

        ResetPasswordTokenModel resetPasswordTokenModel = resetPasswordTokenService.findByUserId(user.getId())
                .orElseThrow(() -> new UserNotFoundException("User did not initiated reset password request."));

        if (!Objects.equals(resetPasswordRequest.getToken(), resetPasswordTokenModel.getToken()))
            throw new InvalidTokenException("Failed to authenticate token. Please re-request to reset password.");

        if (!resetPasswordTokenService.isTokenValid(resetPasswordTokenModel))
            throw new InvalidTokenException("Token expired. Please re-request to reset password");

        String newPassword = passwordEncoder.encode(resetPasswordRequest.getPassword());
        repository.updatePassword(resetPasswordRequest.getEmail(), newPassword);
        resetPasswordTokenService.deleteResetPasswordToken(resetPasswordTokenModel);
        return new ResponseBody<>(SUCCESS, "", Constants.RESET_PASSWORD_SUCCESS_MESSAGE);
    }

    @Override
    public ResponseBody<String> forgotPassword(String email, HttpServletRequest servletRequest) {
        UserModel user = repository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        String resetUrl = getURL(servletRequest) + "/reset-password";
        String refreshToken = resetPasswordTokenService.createResetPasswordToken(user.getId()).getToken();
        String resetPasswordLink = resetUrl + "?token=" + refreshToken + "&email=" + email;
        sendMail(resetPasswordLink, email);
        return new ResponseBody<>(SUCCESS, "", Constants.FORGOT_PASSWORD_SUCCESS_MESSAGE);
    }

    @Override
    public ResponseBody<String> signOut(String token) {
        return blackListTokenService.addBlackListToken(token);
    }

    /**
     * Provides the URL for resetting password
     *
     * @param request HttpServletRequest instance
     * @return String representing URL to reset password
     */
    private String getURL(HttpServletRequest request) {
        String siteURL = request.getRequestURL().toString().replace(request.getServletPath(), "");
        try {
            java.net.URL oldURL = new java.net.URL(siteURL);
            java.net.URL newURL = new java.net.URL(oldURL.getProtocol(), oldURL.getHost(), frontendPort, oldURL.getFile());
            return newURL.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to change URL port", e);
        }
    }

    /**
     * Sends the mail for resetting password to requesting email with provided reset password link
     *
     * @param resetPasswordLink String representing link to reset password
     * @param email             Email of the user
     */
    private void sendMail(String resetPasswordLink, String email) {
        String subject = "Reset your password";
        String content = "<p>You have requested to reset your password. Password link only valid for 10 minutes.</p>"
                + "<p>Click the link to reset your password:</p>"
                + "<p><a href=\"" + resetPasswordLink + "\">Reset my password</a></p>";
        try {
            emailSender.sendEmail(email, subject, content);
        } catch (Exception exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }

    public void subscribeToEventNotifications(String email) {
        String snsTopicArn = ensureTopicExists();
        SubscribeRequest subscribeRequest = SubscribeRequest.builder()
                .protocol("email")
                .endpoint(email)
                .topicArn(snsTopicArn)
                .build();

        snsClient.subscribe(subscribeRequest);
    }

    private String ensureTopicExists() {
        String topicArnPrefix = "arn:aws:sns:" + awsProperties.region + ":" + awsProperties.accountId + ":";
        String topicName = "ServiceHubAllUsers";
        ListTopicsResponse listTopicsResponse = snsClient.listTopics();
        for (Topic topic : listTopicsResponse.topics()) {
            if (topic.topicArn().equals(topicArnPrefix + topicName)) {
                return topic.topicArn();
            }
        }

        CreateTopicRequest createTopicRequest = CreateTopicRequest.builder()
                .name(topicName)
                .build();
        CreateTopicResponse createTopicResponse = snsClient.createTopic(createTopicRequest);
        return createTopicResponse.topicArn();
    }
}
