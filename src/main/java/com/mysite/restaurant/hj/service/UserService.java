package com.mysite.restaurant.hj.service;

import com.mysite.restaurant.hj.domain.dto.UserLoginRequestDto;
import com.mysite.restaurant.hj.domain.entity.UserEntity;
import com.mysite.restaurant.hj.mapper.UserMapper;
import com.mysite.restaurant.hj.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public UserService(PasswordEncoder passwordEncoder, UserMapper userMapper, JwtTokenProvider jwtTokenProvider) {
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String login(UserLoginRequestDto loginRequest) {
        // 사용자 ID로 데이터베이스에서 사용자 조회
        Optional<UserEntity> user = userMapper.findUserByUserName(loginRequest.getUserName());
        UserEntity userEntity = user.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String storedPassword = userEntity.getPassword();

        // 비밀번호 검증
        if (storedPassword == null || !passwordEncoder.matches(loginRequest.getPassword(), storedPassword)) {
            throw new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // JWT 생성 및 반환
        return jwtTokenProvider.generateToken(userEntity);
    }

    public UserEntity getUserByUserName(String userName) {
        Optional<UserEntity> result = userMapper.findUserByUserName(userName);
        return result.orElseThrow();
    }

    // 모든 멤버 가져오기
    public List<Map<String, Object>> getAllUsers() {
        return userMapper.findAllUsers();
    }
}