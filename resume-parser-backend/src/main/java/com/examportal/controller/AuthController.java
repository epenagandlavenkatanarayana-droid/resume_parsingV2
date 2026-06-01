package com.examportal.controller;

import com.examportal.payload.ApiResponse;
import com.examportal.payload.AuthResponse;
import com.examportal.payload.OtpLoginRequest;
import com.examportal.payload.OtpRequest;
import com.examportal.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse> sendOtp(@Valid @RequestBody OtpRequest otpRequest) {
        authService.sendOtp(otpRequest);
        return new ResponseEntity<>(new ApiResponse("OTP sent successfully.", true), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody OtpLoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
