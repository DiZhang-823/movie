package com.example.movie.service;

import com.example.movie.dto.auth.LoginRequest;
import com.example.movie.dto.auth.LoginResponse;
import com.example.movie.dto.auth.RegisterRequest;
import com.example.movie.dto.auth.RegisterResponse;
import com.example.movie.entity.User;
import com.example.movie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailDeliveryService emailDeliveryService;
    private final Map<String, EmailCodeInfo> emailCodeMap = new ConcurrentHashMap<>();

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailDeliveryService emailDeliveryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已被占用");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("电子邮箱已被占用");
        }

        EmailCodeInfo codeInfo = emailCodeMap.get(request.getEmail());

        if (codeInfo == null) {
            throw new RuntimeException("请先获取邮箱验证码");
        }

        if (System.currentTimeMillis() > codeInfo.expireTime()) {
            emailCodeMap.remove(request.getEmail());
            throw new RuntimeException("邮箱验证码已过期，请重新获取");
        }

        if (!codeInfo.code.equals(request.getEmailCode())) {
            throw new RuntimeException("邮箱验证码错误");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEnabled(true);
        user.setAdmin(false);

        User savedUser = userRepository.save(user);

        emailCodeMap.remove(request.getEmail());
        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getNickname()
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Username or Password is incorrect"
                ));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("User is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or Password is incorrect");
        }

        String token = jwtService.generateToken(user.getUsername());
        return new LoginResponse(token);
    }

    public void sendEmailCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被使用");
        }

        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        long expireTime = System.currentTimeMillis() + 5 * 60 * 1000;
        emailCodeMap.put(email, new EmailCodeInfo(code, expireTime));

        try {
            log.info("Sending verification code email to {}", email);
            emailDeliveryService.sendVerificationCode(email, code);
            log.info("Verification code email sent to {}", email);
        } catch (EmailDeliveryException ex) {
            emailCodeMap.remove(email);
            log.error("Failed to send verification code email to {}", email, ex);
            throw new RuntimeException(ex.getMessage());
        }
    }

    private record EmailCodeInfo(String code, long expireTime) {
    }
}
