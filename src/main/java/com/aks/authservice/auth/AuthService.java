package com.aks.authservice.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aks.authservice.security.JwtService;
import com.aks.authservice.user.RoleType;
import com.aks.authservice.user.UserEntity;
import com.aks.authservice.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already used");
        }
        if (userRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("EmployeeId already used");
        }

        Set<RoleType> roles = (request.getRoles() == null || request.getRoles().isEmpty())
                ? Set.of(RoleType.ROLE_EMPLOYEE)
                : request.getRoles();

        UserEntity user = UserEntity.builder()
                .employeeId(request.getEmployeeId())
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .enabled(true)
                .build();

        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()
                )
        );

        // principal's username = email
        String email = authentication.getName();

        Map<String, Object> claims = new HashMap<>();
        // add roles into token so gateway and other services can use
        claims.put("roles", authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList());

        String token = jwtService.generateToken(email, claims);

        return new AuthResponse(token, "Bearer", 3600L);
    }
}

