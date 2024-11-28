package com.mysite.restaurant.hj.service;

import java.util.Optional;

import com.mysite.restaurant.hj.dto.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mysite.restaurant.hj.dto.UserDTO;
import com.mysite.restaurant.hj.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private  UserMapper userMapper;
	
//	회원가입
	public int save (UserDTO user) {
		return userMapper.save(user);
	}

	@Override
	public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
		Optional<UserDTO> user = userMapper.selectByUserName(userName);
		if (user.isEmpty()) {
			throw new UsernameNotFoundException("User not found with userName: " + userName);
		}
		return new CustomUserDetails(user.orElse(null));
	}
	
//	로그인
	public UserDTO findByUserId(String email) {
		return userMapper.selectByUserId(email);
	}
	
//	로그아웃
	
//	사용자 정보 조회
	public UserDTO selectUserProfile(String email) {
        // 데이터베이스에서 유저 조회
        UserDTO user = userMapper.selectUserProfile(email);

        if (user == null) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }

        return user;
    }
	
//	사용자 정보 수정
	public int updateUserProfile(UserDTO user) {
		return userMapper.updateUserProfile(user);
	}
	
//	회원 탈퇴
	public int deleteUser(String email) {
		return userMapper.deleteUser(email);
	}
	
//	소셜 로그인
	
//	이메일 인증
	
//	휴대폰 인증
	
//	사업자 인증
	
//	사업자 인증 후 사용자 정보 수정
	
}
