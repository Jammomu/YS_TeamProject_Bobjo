package com.mysite.restaurant.hj.service;

import com.mysite.restaurant.hj.domain.dto.SignupRequestDto;
import com.mysite.restaurant.hj.domain.entity.UserAuthEntity;
import com.mysite.restaurant.hj.domain.entity.UserEntity;
import com.mysite.restaurant.hj.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserMapper userMapper; // 회원 정보를 처리할 매퍼
    private final PasswordEncoder passwordEncoder; // 비밀번호 암호화

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public long registerUser(SignupRequestDto signupRequestDto) {
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequestDto.getPassword());

        UserEntity userEntity = UserEntity.builder()
                .userName(signupRequestDto.getUserName())
                .password(encodedPassword)
                .name(signupRequestDto.getName())
                .email(signupRequestDto.getEmail())
                .phone(signupRequestDto.getPhone())
                .build();

        // 회원 정보 저장
        userMapper.insertUser(userEntity);

        UserAuthEntity userAuthEntity = UserAuthEntity.builder()
                .userId(userEntity.getUserId())
                .auth("ROLE_USER")
                .build();

        // 권한 저장
        userMapper.insertUserAuth(userAuthEntity);

        // 등록 순번 반환
        return userEntity.getUserId();
    }
}