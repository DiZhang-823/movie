package com.example.movie.service;

import com.example.movie.dto.auth.LoginRequest;
import com.example.movie.dto.auth.LoginResponse;
import com.example.movie.dto.auth.RegisterRequest;
import com.example.movie.dto.auth.RegisterResponse;
import com.example.movie.entity.User;
import com.example.movie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JavaMailSender mailSender;
    private final String mailFrom;
    private final Map<String, EmailCodeInfo> emailCodeMap = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JavaMailSender mailSender,
                       @Value("${spring.mail.username}") String mailFrom) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailSender = mailSender;
        this.mailFrom = mailFrom;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username or Password is incorrect"));

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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("电影站注册验证码");
        message.setText("""
                您正在注册电影站账号。

                验证码：%s

                验证码 5 分钟内有效。如果不是您本人操作，请忽略这封邮件。
                """.formatted(code));

        try {
            log.info("Sending verification code email to {}", email);
            mailSender.send(message);
            log.info("Verification code email sent to {}", email);
        } catch (MailAuthenticationException ex) {
            emailCodeMap.remove(email);
            log.error("Failed to authenticate with mail server when sending verification code to {}", email, ex);
            throw new RuntimeException("邮箱服务认证失败，请检查发件邮箱账号和应用专用密码");
        } catch (MailException ex) {
            emailCodeMap.remove(email);
            log.error("Failed to send verification code email to {}", email, ex);
            throw new RuntimeException("验证码发送失败，请稍后重试");
        }
    }

    private record EmailCodeInfo(String code, long expireTime) {
    }
}

