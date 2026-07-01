package davi.spf.supportflow.auth.service;

import davi.spf.supportflow.auth.dto.LoginRequest;
import davi.spf.supportflow.auth.dto.LoginResponse;
import davi.spf.supportflow.common.exception.BusinessRuleException;
import davi.spf.supportflow.user.entity.User;
import davi.spf.supportflow.user.enums.UserStatus;
import davi.spf.supportflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTService jwtService;

    public LoginResponse login (LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(()-> new BadCredentialsException("Invalid email or password"));

        if(user.getStatus()!= UserStatus.ACTIVE){
            throw new BusinessRuleException("User is not active");
        }

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())){
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);

        return new LoginResponse(accessToken);
    }
}
