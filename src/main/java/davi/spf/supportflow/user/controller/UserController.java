package davi.spf.supportflow.user.controller;

import davi.spf.supportflow.user.dto.UserRequestDTO;
import davi.spf.supportflow.user.dto.UserResponseDTO;
import davi.spf.supportflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
@Tag(name = "2. Users", description = "Administração de usuários")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Lista usuários")
    public ResponseEntity<Page<UserResponseDTO>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca usuário por id")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @PostMapping("/signup")
    @Operation(summary = "Cria usuário")
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody @Valid UserRequestDTO dto) {
        UserResponseDTO response = userService.createUser(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Bloqueia usuário")
    public ResponseEntity<Void> blockUser(@PathVariable Long id, Authentication authentication) {
        userService.blockUser(id, authentication);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Desbloqueia usuário")
    public ResponseEntity<Void> unblockUser(@PathVariable Long id, Authentication authentication) {
        userService.unblockUser(id, authentication);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/delete")
    @Operation(summary = "Realiza soft delete do usuário")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        userService.deleteUser(id, authentication);

        return ResponseEntity.noContent().build();
    }
}
