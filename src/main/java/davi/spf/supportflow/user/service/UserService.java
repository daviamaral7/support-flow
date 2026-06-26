package davi.spf.supportflow.user.service;

import davi.spf.supportflow.user.dto.UserResponseDTO;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.mapper.UserMapper;
import davi.spf.supportflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> listUsers(Pageable pageable) {
        return userRepository.findAllByStatusNot(UserStatus.DELETED, pageable).map(mapper::toResponse);
    }
}
