package com.example.movie.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 6, max = 50, message = "用户名长度为6-50个字符")
    private String username;

    @NotBlank(message = "电子邮箱不能为空")
    @Email(message = "电子邮箱格式有误")
    @Size(max = 100, message = "电子邮箱长度不能超过100个字符")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100个字符")
    private String password;

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @NotBlank(message = "邮箱验证码不能为空")
    private String emailCode;
}