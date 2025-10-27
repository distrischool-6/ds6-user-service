package com.ds6.serviceimpl;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ds6.dto.LoginRequestDTO;
import com.ds6.dto.LoginResponseDTO;
import com.ds6.dto.RegisterRequestDTO;
import com.ds6.dto.UserResponseDTO;
import com.ds6.model.User;
import com.ds6.repository.UserRepository;
import com.ds6.service.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtServiceImpl jwtService;
    private final AuthenticationManager authenticationManager;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public UserResponseDTO registerUser(RegisterRequestDTO request) {

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        User savedUser = userRepository.save(user);

        return new UserResponseDTO(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
    }

    @Override
    public LoginResponseDTO loginUser(LoginRequestDTO request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String jwtToken = jwtService.generateToken(userDetails);

        try {
            String topic = "user.logged";
            String payload = userDetails.getUsername();

            kafkaTemplate.send(topic, payload);
            log.info("Audit event 'user.logged' published for user: {}", payload);
        } catch (Exception e) {
            log.error("Failed to publish 'user.logged' audit event for user: {}. Error: {}", userDetails.getUsername(), e.getMessage());
        }

        return new LoginResponseDTO(jwtToken);
    }
}
