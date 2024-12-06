package com.mysite.restaurant;

import com.mysite.restaurant.hj.domain.dto.SignupRequestDto;
import com.mysite.restaurant.hj.mapper.UserMapper;
import com.mysite.restaurant.hj.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserMapper userMapper;

    @Test
    void testRegisterUser() {
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
        long userId = authService.registerUser(signupRequestDto);

        // Then
        assertThat(userId).isGreaterThan(0); // 생성된 아이디가 0보다 큰지 확인
    }
}

