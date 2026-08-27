package com.mapletalk.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/**
 * Narrowly scoped JWT primitive: generate a token for a subject (the user's
 * email — see {@code UserRepository.findByEmail}) and validate/parse one back.
 * Contains no authentication or business logic; that belongs to the filter
 * and, later, the auth service.
 */
@Service
public class JwtService {

	private final SecretKey signingKey;
	private final long expirationMs;

	public JwtService(
			@Value("${security.jwt.secret}") String secret,
			@Value("${security.jwt.expiration-ms}") long expirationMs) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(String subject) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expirationMs);

		return Jwts.builder()
				.subject(subject)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(signingKey)
				.compact();
	}

	public String extractSubject(String token) {
		return parseClaims(token).getSubject();
	}

	public Date extractExpiration(String token) {
		return parseClaims(token).getExpiration();
	}

	public boolean isTokenExpired(String token) {
		try {
			return extractExpiration(token).before(new Date());
		} catch (ExpiredJwtException ex) {
			return true;
		}
	}

	/**
	 * Returns whether the token is well-formed, correctly signed, and not
	 * expired. Never throws — callers (the JWT filter) must not let a bad
	 * token turn into an unhandled exception / HTTP 500.
	 */
	public boolean validateToken(String token) {
		try {
			return !isTokenExpired(token);
		} catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

}
