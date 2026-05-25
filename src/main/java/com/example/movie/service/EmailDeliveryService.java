package com.example.movie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final RestTemplate restTemplate;
    private final String resendApiUrl;
    private final String resendApiKey;
    private final String mailFrom;

    public EmailDeliveryService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${resend.api-url}") String resendApiUrl,
            @Value("${resend.api-key}") String resendApiKey,
            @Value("${resend.from}") String mailFrom,
            @Value("${resend.timeout-ms}") long timeoutMs) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .readTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.resendApiUrl = resendApiUrl;
        this.resendApiKey = resendApiKey;
        this.mailFrom = mailFrom;
    }

    public void sendVerificationCode(String email, String code) {
        if (!hasText(resendApiKey)) {
            throw new EmailDeliveryException("邮件服务未配置 RESEND_API_KEY");
        }
        if (!hasText(mailFrom)) {
            throw new EmailDeliveryException("邮件发件人未配置 RESEND_FROM");
        }

        Map<String, Object> payload = Map.of(
                "from", mailFrom,
                "to", List.of(email),
                "subject", "电影站注册验证码",
                "text", buildVerificationEmailText(code)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resendApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.USER_AGENT, "movie-app/1.0");

        try {
            restTemplate.postForEntity(resendApiUrl, new HttpEntity<>(payload, headers), String.class);
        } catch (RestClientResponseException ex) {
            log.error(
                    "Resend API rejected verification email to {} with status {} and body {}",
                    email,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString(),
                    ex
            );
            throw new EmailDeliveryException("验证码发送失败，请稍后重试", ex);
        } catch (ResourceAccessException ex) {
            log.error("Failed to connect to Resend API when sending verification email to {}", email, ex);
            throw new EmailDeliveryException("邮件服务连接失败，请稍后重试", ex);
        }
    }

    private String buildVerificationEmailText(String code) {
        return """
                您正在注册电影站账号。
                验证码：%s

                验证码 5 分钟内有效。如果不是您本人操作，请忽略这封邮件。
                """.formatted(code);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
