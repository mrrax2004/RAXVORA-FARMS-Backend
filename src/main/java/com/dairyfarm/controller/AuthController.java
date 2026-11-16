package com.dairyfarm.controller;

import com.dairyfarm.dto.AuthDtos;
import com.dairyfarm.service.AuthService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }
    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) { return auth.login(request); }
    @GetMapping("/me")
    public AuthDtos.UserResponse me(Principal principal) { return auth.me(principal.getName()); }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Principal principal) {
        auth.logout(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
