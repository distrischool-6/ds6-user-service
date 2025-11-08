package com.ds6.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds6.dto.LoginRequestDTO;
import com.ds6.dto.LoginResponseDTO;
import com.ds6.dto.RegisterRequestDTO;
import com.ds6.dto.UserResponseDTO;
import com.ds6.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    public final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerUser(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        UserResponseDTO registeredUser = userService.registerUser(registerRequest);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        LoginResponseDTO loginResponse = userService.loginUser(loginRequest);
        return new ResponseEntity<>(loginResponse, HttpStatus.OK);
    }
}
