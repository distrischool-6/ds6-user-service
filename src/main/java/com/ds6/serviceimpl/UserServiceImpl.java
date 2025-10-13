package com.ds6.serviceimpl;

import java.util.UUID;

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

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtServiceImpl jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public UserResponseDTO registerUser(RegisterRequestDTO request) {
        // 1. Verifica se o email já existe
        if (userRepository.findByEmail(request.email()).isPresent()) {
            // No futuro, podemos criar uma exceção customizada aqui
            throw new IllegalArgumentException("Email already in use");
        }

        // 2. Cria uma nova entidade User
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        // 3. Codifica a senha antes de a salvar
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());

        // 4. Salva o novo utilizador na base de dados
        User savedUser = userRepository.save(user);

        // 5. Retorna o DTO de resposta
        return new UserResponseDTO(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
    }

    @Override
    public LoginResponseDTO loginUser(LoginRequestDTO request) {
        // 1. Usa o AuthenticationManager para validar as credenciais
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // Se a autenticação for bem-sucedida, o Spring Security guarda os detalhes do utilizador
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. Gera o token JWT para o utilizador autenticado
        String jwtToken = jwtService.generateToken(userDetails);

        // 3. Retorna o token na resposta
        return new LoginResponseDTO(jwtToken);
    }
}
