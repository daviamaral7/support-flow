package davi.spf.supportflow.auth.controller;

import davi.spf.supportflow.auth.dto.AuthenticatedUserResponse;
import davi.spf.supportflow.auth.dto.LoginRequest;
import davi.spf.supportflow.auth.dto.LoginResponse;
import davi.spf.supportflow.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@RequestBody @Valid LoginRequest loginRequest) {

        return authService.login(loginRequest);
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(Authentication authentication) {
        return authService.me(authentication);
    }
}
