package com.mysite.restaurant.hj.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import com.mysite.restaurant.hj.dto.CustomUserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

	@Value("${jwt.secret}")
	private String secretKey;
	
	@Value("${jwt.token-validity-in-seconds}")
	private long tokenValidityInSeconds;

	private SecretKey key;
	
//	생성자(Construct)가 호출된 후(Post)에 실행 또는
//	의존성 주입이 모두 완료된 후 실행 또는
//	해당 클래스가 스프링 빈으로 등록될 때 한 번만 실행 <== 이 코드에서는 여기에 해당
//	init(): 객체의 초기화를 위한 인스턴스 초기화 메서드
	@PostConstruct
	public void init() {
		byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
		this.key = Keys.hmacShaKeyFor(keyBytes);
	}
	
	public String createToken(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		
		String authorities = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
//				.map(auth -> auth.replaceFirst("^ROLE_", "")) < ROLE_ 제거하는 건데 제거할경우 JWT auth 파싱해서 Authentication 객체에 설정해야함
				.collect(Collectors.joining(","));
		
		long now = System.currentTimeMillis();
		Date validity = new Date(now + this.tokenValidityInSeconds * 1000);

		return Jwts.builder()
//				Header
				.signWith(key, SignatureAlgorithm.HS512) // , SignatureAlgorithm.HS512 제거
//				Payload
//				-- Registered Claims
				.subject(userDetails.getUsername())	// 로그인시 사용되는 식별자(보통 이메일이나 아이디)
				.issuedAt(new Date(now))
				.expiration(validity)
//				-- Custom Claims
				.claim("name", userDetails.getUser().getName()) // User에 정의된 이름(실명 또는 닉네임)
				.claim("userId", userDetails.getUser().getUserId())
				.claim("restaurantId", userDetails.getUser().getRestaurants())
				.claim("auth", authorities)
				.compact();
	}
	
	public Authentication getAuthentication(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		
		Collection <? extends GrantedAuthority> authorities = 
				Arrays.stream(claims.get("auth").toString().split(","))
					.map(SimpleGrantedAuthority::new)
					.collect(Collectors.toList());
		
		User principal = new User(claims.getSubject(), 	"", authorities);
		
		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}

	public Long getUserId(String token) {
		Claims claims = parseClaims(token);
		return claims.get("userId", Long.class);
	}

	private Claims parseClaims(String token) {
		return Jwts.parser() // parserBuilder() 사용
				.setSigningKey(key)
				.build() // 빌드하여 JwtParser 생성
				.parseClaimsJws(token) // claims 파싱
				.getBody();
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(key) // 서명 검증
				.build()
				.parseSignedClaims(token); // 토큰 구조 검증(jwt 형식, 파싱 가능 여부)
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.error("유효하지 않은 JWT 토큰: {}", e.getMessage());
			return false;
		}
	}
}
