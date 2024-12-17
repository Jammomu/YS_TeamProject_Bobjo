package com.mysite.restaurant.hj.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.mysite.restaurant.hj.domain.entity.UserStatus;
import com.mysite.restaurant.hj.domain.entity.UserType;
import com.mysite.restaurant.jh.RestaurantDTO;

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
	private String password;
	private String name;
	private String email;
	private String phone;
	private String profileImageUrl; // 사용자 프로필 이미지 URL
	private Integer notificationAgreed; // 알림 수신 동의
	private UserType userType;			// 사용자 권한
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private int isDeleted;
	private UserStatus status;

	private List<RestaurantDTO> restaurants;
	private String restaurantName;

	private List<UserAuthDTO> authorities;
	
	public UserDTO(String restaurantName) {
		this.restaurantName = restaurantName;
	}
	
	public String getAuth() {
        if (authorities != null && !authorities.isEmpty()) {
            return authorities.get(0).getAuth();
        }
        return null;
    }
	public void setAuth(String auth) {
		if (authorities == null) {
			authorities = new ArrayList<>();
		}

		// 중복 권한 방지
		boolean authExists = authorities.stream()
				.anyMatch(userAuth -> auth.equals(userAuth.getAuth()));

		if (!authExists) {
			authorities.add(UserAuthDTO.builder()
					.userId(this.userId)
					.auth(auth)
					.build());
		}
	}

}
