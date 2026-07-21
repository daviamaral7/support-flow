package davi.spf.supportflow.auth.controller;

import davi.spf.supportflow.auth.dto.AuthenticatedUserResponse;
import davi.spf.supportflow.auth.dto.LoginRequest;
import davi.spf.supportflow.auth.dto.LoginResponse;
import davi.spf.supportflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "1. Auth", description = "Autenticação e usuário autenticado")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Realiza login e retorna token JWT")
    public LoginResponse login(@RequestBody @Valid LoginRequest loginRequest) {

        return authService.login(loginRequest);
    }

    @GetMapping("/me")
    @Operation(summary = "Retorna dados do usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public AuthenticatedUserResponse me(Authentication authentication) {
        return authService.me(authentication);
    }
}
