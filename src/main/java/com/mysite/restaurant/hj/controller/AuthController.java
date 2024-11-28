package com.mysite.restaurant.hj.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mysite.restaurant.hj.dto.CustomUserDetails;
import com.mysite.restaurant.hj.dto.JsonResponse;
import com.mysite.restaurant.hj.dto.LoginRequest;
import com.mysite.restaurant.hj.dto.SignupRequest;
import com.mysite.restaurant.hj.dto.TokenResponse;
import com.mysite.restaurant.hj.dto.UserDTO;
import com.mysite.restaurant.hj.jwt.JwtTokenProvider;
import com.mysite.restaurant.hj.service.CustomUserDetailsService;
import com.mysite.restaurant.hj.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

	private final AuthenticationManagerBuilder authenticationManagerBuilder;
	private final JwtTokenProvider tokenProvider;
	private final UserService userService;
	private final CustomUserDetailsService customUserDetailsService;
	private final PasswordEncoder passwordEncoder;

//	@Autowired
//	private CustomUserDetailsService customUserDetailsService;
	
//	회원가입 처리
//	@PostMapping("/signup")
//	public int signup (@RequestBody User user) {
//		return customUserDetailsService.save(user);
//	}
	
//	@PostMapping("/signup")
//	public ResponseEntity<JsonResponse> signup(@Valid @RequestBody SignupRequest request) {
//		if (userService.existsByUserId(request.getUserId())) {
//			throw new DuplicateKeyException("이미 사용중인 아이디입니다.");
//		}
//		
//		UserDTO member = request.toUserDTO(passwordEncoder);
//		userService.signup(member);
//		
//		return ResponseEntity
//				.status(HttpStatus.CREATED)
//				.body(JsonResponse.builder()
//						.success(true)
//						.message("회원가입이 완료되었습니다.")
//						.build());
//	}
	
	@PostMapping("/signup")
	public ResponseEntity<JsonResponse> signup(@Valid @RequestBody SignupRequest request) {
	    // 비밀번호 암호화 및 DTO 변환
	    UserDTO member = request.toUserDTO(passwordEncoder);

	    // 회원 가입 처리
	    userService.signup(member);

	    return ResponseEntity
	            .status(HttpStatus.CREATED)
	            .body(JsonResponse.builder()
	                    .success(true)
	                    .message("회원가입이 완료되었습니다.")
	                    .build());
	}

//  로그인
//    @PostMapping("/login")
//    public UserDTO login(@RequestBody String userId) {
//    	return customUserDetailsService.findByUserId(userId);
//    }
	
	@PostMapping("/login")
    public ResponseEntity<JsonResponse> login(@Valid @RequestBody LoginRequest request) {
    	UsernamePasswordAuthenticationToken authenticationToken =
    			new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword());
    	
    	Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
    	SecurityContextHolder.getContext().setAuthentication(authentication);
    	
    	String jwt = tokenProvider.createToken(authentication);
    	CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
    	UserDTO user = userDetails.getUser();
    	
    	userService.updateLastLogin(user.getUserId());
    	
    	TokenResponse tokenResponse = TokenResponse.builder()
    			.token(jwt)
    			.email(user.getEmail())
    			.userName(user.getName())
    			.build();
    	
    	return ResponseEntity.ok(JsonResponse.builder()
    			.success(true)
    			.message("로그인이 완료되었습니다.")
    			.data(tokenResponse)
    			.build());
    }
    
//  로그아웃
    
//	사용자 정보 조회
    @GetMapping("/me/{user_id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable("email") String email) {
        UserDTO user = customUserDetailsService.selectUserProfile(email);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        return ResponseEntity.ok(user);
    }
    
//  사용자 정보 수정
    @PutMapping("/me")
    public int updateUserProfile(@RequestBody UserDTO user) {
    	return customUserDetailsService.updateUserProfile(user);
    }
    
//  회원 탈퇴
    @DeleteMapping("/deleteUser")
    public int deleteUser(@RequestParam("email") String email) {
    	return customUserDetailsService.deleteUser(email);
    }
    
}
