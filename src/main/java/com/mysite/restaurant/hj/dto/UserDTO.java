package com.mysite.restaurant.hj.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

	private Long userId;
	private String userName;
	private String email;
	private String password;
	private String name;
	private String phone;
	private String profileImageUrl;
	private Integer notificationAgreed;
	private UserType userType;			// 사용자 권한
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private int isDeleted;
	private Status status;
	
	private List<UserAuthDTO> authorities;
	
	public enum UserType {
		CUSTOMER,
		OWNER,
		USER
	}

	public enum Status {
		ACTIVE
	}
}
