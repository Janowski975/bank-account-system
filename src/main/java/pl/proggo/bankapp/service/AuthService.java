package pl.proggo.bankapp.service;

import pl.proggo.bankapp.dto.AuthResponse;
import pl.proggo.bankapp.dto.LoginRequest;
import pl.proggo.bankapp.dto.RegisterRequest;
import pl.proggo.bankapp.entity.User;
import pl.proggo.bankapp.exception.BusinessException;
import pl.proggo.bankapp.repository.UserRepository;
import pl.proggo.bankapp.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already exists: " + request.getEmail());
        }

        // Validate email format
        if (!request.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BusinessException("Invalid email format");
        }

        // Validate password strength
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long");
        }
        if (!request.getPassword().matches(".*[A-Z].*")) {
            throw new BusinessException("Password must contain at least one uppercase letter");
        }
        if (!request.getPassword().matches(".*[a-z].*")) {
            throw new BusinessException("Password must contain at least one lowercase letter");
        }
        if (!request.getPassword().matches(".*\\d.*")) {
            throw new BusinessException("Password must contain at least one number");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole("USER");
        user.setIsActive(true);

        User savedUser = userRepository.save(user);

        String accessToken = jwtUtils.generateAccessToken(savedUser.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(savedUser.getUsername());

        log.info("User registered successfully: {}", savedUser.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getUsername(),
                savedUser.getEmail(),
                3600L
        );
    }

    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        if (!user.getIsActive()) {
            throw new BusinessException("Account is locked or inactive");
        }

        String accessToken = jwtUtils.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getUsername());

        log.info("User logged in successfully: {}", user.getUsername());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getUsername(),
                user.getEmail(),
                3600L
        );
    }
}