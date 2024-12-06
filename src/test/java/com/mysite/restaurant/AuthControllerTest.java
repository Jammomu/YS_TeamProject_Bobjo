package com.mysite.restaurant;

import com.mysite.restaurant.hj.controller.AuthController;
import com.mysite.restaurant.hj.domain.dto.SignupRequestDto;
import com.mysite.restaurant.hj.domain.dto.UserLoginRequestDto;
import com.mysite.restaurant.hj.domain.entity.UserEntity;
import com.mysite.restaurant.hj.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthControllerTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testSignup() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String nowString = now.format(formatter);
        SignupRequestDto signupRequestDto = SignupRequestDto.builder()
                .userName("testUser" + nowString)
                .password("1234")
                .name("Test Name" + nowString)
                .email("test" + nowString + "@example.com")
                .phone("123-456-7890")
                .build();

        // When
        ResponseEntity<String> response = authController.signup(signupRequestDto);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(201);
        assertThat(response.getBody()).isEqualTo("회원가입이 완료되었습니다.");

        // 데이터베이스에 사용자가 저장되었는지 확인
        UserEntity userEntity = userMapper.findUserByUserName("testUser" + nowString).orElse(null);
        assertThat(userEntity).isNotNull();
        assertThat(userEntity.getEmail()).isEqualTo("test" + nowString + "@example.com");
    }

    @Test
    void testLogin() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String nowString = now.format(formatter);
        String encodedPassword = passwordEncoder.encode("1234");
        UserEntity userEntity = UserEntity.builder()
                .userName("testUser" + nowString)
                .password(encodedPassword)
                .name("Test Name" + nowString)
                .email("test" + nowString + "@example.com")
                .phone("123-456-7890")
                .build();
        userMapper.insertUser(userEntity);

        UserLoginRequestDto loginRequestDto = UserLoginRequestDto.builder()
                .userName("testUser" + nowString)
                .password("1234")
                .build();

        HttpServletResponse mockResponse = new MockHttpServletResponse();

        // When
        ResponseEntity<String> response = authController.login(loginRequestDto, mockResponse);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("로그인이 성공으로 완료되었습니다.");
        String cookie = Objects.requireNonNull(mockResponse.getHeader("Set-Cookie"));
        assertThat(cookie).contains("JWT");
    }

    @Test
    void testLogout() {
        // Given
        HttpServletResponse mockResponse = new MockHttpServletResponse();

        // When
        ResponseEntity<String> response = authController.logout(mockResponse);

        // Then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("로그아웃되었습니다.");
        String cookie = Objects.requireNonNull(mockResponse.getHeader("Set-Cookie"));
        assertThat(cookie).contains("Max-Age=0"); // 만료된 쿠키
    }
}
