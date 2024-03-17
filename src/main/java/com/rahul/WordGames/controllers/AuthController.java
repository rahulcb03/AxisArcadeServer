package com.rahul.wordgames.controllers;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

import com.rahul.wordgames.dto.JwtAuthResponse;
import com.rahul.wordgames.dto.RefreshTokenRequest;
import com.rahul.wordgames.dto.SignInRequest;
import com.rahul.wordgames.dto.SignUpRequest;

import com.rahul.wordgames.entities.User;
import com.rahul.wordgames.repos.UserRepository;
import com.rahul.wordgames.services.AuthService;
import com.rahul.wordgames.services.EmailService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin
public class AuthController {

    private final AuthService authenticationService; 
    private final UserRepository userRepository;
    private final EmailService emailService; 
    private final PasswordEncoder passwordEncoder;

     
    @Value("${BASE_URL}") String baseUrl;


    @PostMapping("/signup")
    public ResponseEntity<User> signUp(@RequestBody SignUpRequest signUpRequest){
        Optional<User> user = authenticationService.signup(signUpRequest);

        if(user.isPresent()){
            return ResponseEntity.ok(user.get());
        } else{
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtAuthResponse> signin(@RequestBody SignInRequest signInRequest){
        Optional<JwtAuthResponse> jwtAuthResponse = authenticationService.signin(signInRequest);
        return jwtAuthResponse.isPresent() ? ResponseEntity.ok(jwtAuthResponse.get()) : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PostMapping("/request-password-reset")
    public ResponseEntity<?> requestPasswordReset( @RequestBody HashMap<String, String> payload) {
        // Check if the email exists in the database
        String email = payload.get("email");
        
        Optional<User> userOptional = userRepository.findUserByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body("Email not found");
        }

        // Generate a unique, secure token for the password reset
        String resetToken = UUID.randomUUID().toString();

        // Save or update the reset token associated with the user in the database
        User user = userOptional.get();
        user.setResetToken(resetToken);
        user.setResetTimeStamp(Instant.now());
        userRepository.save(user);

        // Send an email to the user with the reset token, ideally as a link they can click
        

        String resetLink = baseUrl + "/reset-password?token=" + resetToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink, user.getUsername());

        return ResponseEntity.ok("Password reset email sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody HashMap<String, String> payload) {
        // Validate the reset token and find the associated user
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        Optional<User> userOptional = userRepository.findUserByResetToken(token);
        
        if (!userOptional.isPresent() || token == null) {
            return ResponseEntity.badRequest().body("Invalid token");
        }

        // Update the user's password and clear the reset token
        User user = userOptional.get();

        if (user.getResetTimeStamp() == null || 
            user.getResetTimeStamp().isBefore(Instant.now().minusSeconds(600))) {
            user.setResetToken(null); // Clear the reset token
            userRepository.save(user);
            return ResponseEntity.badRequest().body("Token expired");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        user.setResetToken(null); // Clear the reset token
        userRepository.save(user);

        return ResponseEntity.ok("Password has been reset successfully");
    }


    @PostMapping("/refresh")
    public ResponseEntity<JwtAuthResponse> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest){
        
        return ResponseEntity.ok(authenticationService.refreshToken(refreshTokenRequest));
    }
    
}
