package com.mysite.restaurant.hj.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.mysite.restaurant.hj.dto.CustomUserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
	
	@PostConstruct
	public void init() {
		byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
		this.key = Keys.hmacShaKeyFor(keyBytes);
	}
	
	public String createToken(Authentication authentication) {
		CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
		
		String authorities = authentication.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));
		
		long now = System.currentTimeMillis();
		Date validity = new Date(now + this.tokenValidityInSeconds * 1000);
		
		return Jwts.builder()
//				Header
				.signWith(key, SignatureAlgorithm.HS512)
//				Payload
//				-- Registered Claims
				.subject(userDetails.getUsername())	// 로그인시 사용되는 식별자(보통 이메일이나 아이디)
				.issuedAt(new Date(now))
				.expiration(validity)
				.claim("userName", userDetails.getUser().getName())
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
		
		User principal = new User();
		
		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}
	
	public boolean validateToken(String token) {
		try {
			Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.error("Invalid JWT token: {}", e.getMessage());
			return false;
		}
	}
}
