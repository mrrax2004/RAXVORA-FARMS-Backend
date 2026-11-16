package com.dairyfarm.service;

import com.dairyfarm.dto.AuthDtos;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.AppUser;
import com.dairyfarm.repository.UserRepository;
import com.dairyfarm.security.JwtService;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository users;
    private final JwtService jwtService;
    public AuthService(AuthenticationManager authenticationManager, UserRepository users, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.jwtService = jwtService;
    }
    @Transactional(readOnly = true)
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        AppUser user = users.findByUsername(request.username())
                .filter(AppUser::isActive).orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("Invalid credentials."));
        return new AuthDtos.LoginResponse(jwtService.generate(user), toResponse(user));
    }
    @Transactional(readOnly = true)
    public AuthDtos.UserResponse me(String username) {
        return users.findByUsername(username).map(AuthService::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found."));
    }
    @org.springframework.transaction.annotation.Transactional
    public void logout(String username) {
        AppUser user = users.findByUsername(username).orElseThrow(() -> new NotFoundException("User not found."));
        user.setTokenVersion(user.getTokenVersion() + 1);
        users.save(user);
    }
    public static AuthDtos.UserResponse toResponse(AppUser user) {
        return new AuthDtos.UserResponse(user.getUuid(), user.getUsername(), user.getFullName(), user.isActive(),
                user.getFarm() == null ? null : user.getFarm().getUuid(),
                user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()));
    }
}
