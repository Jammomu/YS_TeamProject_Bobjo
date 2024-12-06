package com.mysite.restaurant;

import com.mysite.restaurant.hj.domain.dto.UserLoginRequestDto;
import com.mysite.restaurant.hj.domain.entity.UserEntity;
import com.mysite.restaurant.hj.mapper.UserMapper;
import com.mysite.restaurant.hj.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testLoginSuccess() {
        // Given
        String encodedPassword = passwordEncoder.encode("1234");

        // 사용자 데이터 삽입
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String nowString = now.format(formatter);
        UserEntity userEntity = UserEntity.builder()
                .userName("testUser" + nowString)
                .password(encodedPassword)
                .name("Test Name" + nowString)
                .email("test" + nowString + "@example.com")
                .phone("123-456-7890")
                .build();
        userMapper.insertUser(userEntity);

        UserLoginRequestDto loginRequest = UserLoginRequestDto.builder()
                .userName("testUser" + nowString)
                .password("1234")
                .build();

        // When
        String token = userService.login(loginRequest);

        // Then
        assertThat(token).isNotNull();
    }

    @Test
    void testLoginFailure() {
        // Given
        String encodedPassword = passwordEncoder.encode("1234");

        // 사용자 데이터 삽입
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String nowString = now.format(formatter);
        UserEntity userEntity = UserEntity.builder()
                .userName("testUser" + nowString)
                .password(encodedPassword)
                .name("Test Name" + nowString)
                .email("test" + nowString + "@example.com")
                .phone("123-456-7890")
                .build();
        userMapper.insertUser(userEntity);

        UserLoginRequestDto loginRequest = UserLoginRequestDto.builder()
                .userName("testUser" + nowString)
                .password("wrongPassword")
                .build();

        // When & Then
        assertThatThrownBy(() -> userService.login(loginRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void testGetAllUsers() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String nowString = now.format(formatter);
        UserEntity userEntity = UserEntity.builder()
                .userName("testUser" + nowString)
                .password(passwordEncoder.encode("1234"))
                .name("Test Name" + nowString)
                .email("test" + nowString + "@example.com")
                .phone("123-456-7890")
                .build();
        userMapper.insertUser(userEntity);

        // When
        List<Map<String, Object>> users = userService.getAllUsers();

        // Then
        assertThat(users).isNotEmpty();
        assertThat(users.get(0).get("user_name")).isEqualTo("testUser" + nowString);
    }
}
