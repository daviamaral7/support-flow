package davi.spf.supportflow.user.service;

import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.common.exception.ResourceAlreadyExistsException;
import davi.spf.supportflow.common.exception.ResourceNotFoundException;
import davi.spf.supportflow.common.exception.UserNotActiveException;
import davi.spf.supportflow.user.dto.UserRequestDTO;
import davi.spf.supportflow.user.dto.UserResponseDTO;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.mapper.UserMapper;
import davi.spf.supportflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> listUsers(Pageable pageable) {
        return userRepository.findAllByStatusNot(UserStatus.DELETED, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findUserById(Long id) {

        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapper.toResponse(user);
    }

    public UserResponseDTO createUser(UserRequestDTO dto) {
        String normalizedEmail = dto.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResourceAlreadyExistsException("Email already in use");
        }

        User user = mapper.toEntity(dto);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(dto.password()));

        User savedUser = userRepository.save(user);

        return mapper.toResponse(savedUser);
    }

    public void blockUser(Long id, Authentication authentication) {
        User adm = getAuthenticatedUser(authentication);

        if (id.equals(adm.getId())) {
            throw new BusinessRuleException("Cannot self block");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessRuleException("Cannot block a deleted user");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new BusinessRuleException("User is already blocked");
        }

        user.setStatus(UserStatus.BLOCKED);
    }

    public void unblockUser(Long id, Authentication authentication) {
        User adm = getAuthenticatedUser(authentication);

        if (id.equals(adm.getId())) {
            throw new BusinessRuleException("Cannot self unblock");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new UserNotActiveException("User is not blocked");
        }

        user.setStatus(UserStatus.ACTIVE);
    }

    public void deleteUser(Long id, Authentication authentication) {
        User adm = getAuthenticatedUser(authentication);

        if (id.equals(adm.getId())) {
            throw new BusinessRuleException("Cannot self delete");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new BusinessRuleException("User already deleted");
        }

        user.setStatus(UserStatus.DELETED);
    }
}
