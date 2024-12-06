package com.mysite.restaurant;

import com.mysite.restaurant.hj.domain.entity.UserAuthEntity;
import com.mysite.restaurant.hj.domain.entity.UserEntity;
import com.mysite.restaurant.hj.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    private UserEntity testUser;
    private UserAuthEntity testUserAuth;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String nowString = now.format(formatter);
        testUser = UserEntity.builder()
                .userName("testUser" + nowString)
                .password("1234")
                .name("Test Name" + nowString)
                .email("test" + nowString + "@example.com")
                .phone("123-456-7890")
                .build();


        testUserAuth = UserAuthEntity.builder()
        .auth("ROLE_USER")
                .build();
    }

    @Test
    void testInsertAndFindUser() {
        // Insert user into the database
        userMapper.insertUser(testUser);
        assertThat(testUser.getUserId()).isNotNull();

        // Fetch user by username
        Optional<UserEntity> foundUser = userMapper.findUserByUserName(testUser.getUserName());
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    void testInsertAndFindUserAuth() {
        // Insert user and its auth
        userMapper.insertUser(testUser);
        testUserAuth.setUserId(testUser.getUserId());
        userMapper.insertUserAuth(testUserAuth);

        // Fetch user auth
        List<UserAuthEntity> userAuthList = userMapper.findUserAuth(testUser.getUserId());
        assertThat(userAuthList).isNotEmpty();
        assertThat(userAuthList.get(0).getAuth()).isEqualTo("ROLE_USER");
    }

    @Test
    void testFindAllUsers() {
        // Insert test user
        userMapper.insertUser(testUser);

        // Fetch all users
        List<Map<String, Object>> users = userMapper.findAllUsers();
        assertThat(users).isNotEmpty();
        assertThat(users.get(0).get("user_name")).isEqualTo(testUser.getUserName());
    }
}