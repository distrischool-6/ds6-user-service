package com.ds6.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desativa a proteção CSRF, pois usaremos JWT (não baseado em sessões)
            .csrf(csrf -> csrf.disable())
            
            // Define a política de gestão de sessão como STATELESS
            // A API não irá guardar estado de sessão no servidor
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configura as regras de autorização para os pedidos HTTP
            .authorizeHttpRequests(auth -> auth
                // Permite o acesso público aos endpoints de registo e login
                .requestMatchers("/api/v1/auth/**").permitAll() 
                // Qualquer outro pedido exige autenticação
                .anyRequest().authenticated() 
            );

        // TODO: Adicionar o filtro de autenticação JWT aqui no futuro

        return http.build();
    }

    // Bean para obter o AuthenticationManager do Spring Security
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
