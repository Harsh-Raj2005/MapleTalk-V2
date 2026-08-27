package com.mapletalk.security;

import java.io.IOException;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mapletalk.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads a Bearer JWT, validates it, and — if valid — populates the
 * SecurityContext with the corresponding user. Does not itself decide what
 * is protected; that is SecurityConfig's job. Never lets a bad/missing token
 * escalate into an exception: it simply leaves the request unauthenticated
 * and continues the chain, so downstream access control returns 401/403.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private static final String AUTH_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		String header = request.getHeader(AUTH_HEADER);

		if (header != null && header.startsWith(BEARER_PREFIX)) {
			String token = header.substring(BEARER_PREFIX.length());

			if (jwtService.validateToken(token)) {
				String email = jwtService.extractSubject(token);

				if (SecurityContextHolder.getContext().getAuthentication() == null
						&& userRepository.existsByEmail(email)) {
					authenticate(email);
				}
			}
		}

		filterChain.doFilter(request, response);
	}

	private void authenticate(String email) {
		// Principal is the email string (not the User entity) so that
		// Authentication#getName() returns the actual user identity rather
		// than falling back to Object#toString().
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				email, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

}
