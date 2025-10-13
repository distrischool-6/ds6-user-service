package com.ds6.service;

import com.ds6.dto.RegisterRequestDTO;
import com.ds6.dto.UserResponseDTO;
import com.ds6.dto.LoginRequestDTO;
import com.ds6.dto.LoginResponseDTO;

public interface UserService {
    UserResponseDTO registerUser(RegisterRequestDTO request);
    LoginResponseDTO loginUser(LoginRequestDTO loginRequest);
}
