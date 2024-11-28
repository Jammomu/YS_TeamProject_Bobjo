package com.mysite.restaurant.hj.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mysite.restaurant.hj.dto.UserAuthDTO;
import com.mysite.restaurant.hj.dto.UserDTO;
import com.mysite.restaurant.hj.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
public class UserService {

	private final UserMapper userMapper;
	
	public boolean existsByUserId(String email) {
		return userMapper.selectByUserId(email) != null;
	}
	
//	@Transactional
//	public void signup(UserDTO user) {
//		userMapper.create(user);
//		
//		UserAuthDTO userAuth = UserAuthDTO.builder()
//				.userId(user.getUserId())
//				.auth("USER")
//				.build();
//		userMapper.createAuth(userAuth);
//	}

	@Transactional
    public void signup(UserDTO userDTO) {
        UserDTO user = new UserDTO();
		user.setUserName(userDTO.getUserName());
        user.setEmail(userDTO.getEmail());
        user.setPassword(userDTO.getPassword());
        user.setName(userDTO.getName());
        user.setPhone(userDTO.getPhone());
		user.setNotificationAgreed(userDTO.getNotificationAgreed());
        // userId는 자동 생성되므로 설정하지 않음
        userMapper.save(user);

		UserAuthDTO userAuth = UserAuthDTO.builder()
				.userId(user.getUserId())
				.auth("ROLE_USER")
				.build();
		userMapper.createAuth(userAuth);
    }
	
	@Transactional
	public void updateLacstLogin(String email) {
		userMapper.selectLastLogin(email);
	}
	
	public Optional<UserDTO> getUserByUsername(String userName) {
		return userMapper.selectByUsername(userName);
	}
}
