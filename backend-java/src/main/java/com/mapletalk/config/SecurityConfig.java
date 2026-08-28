package com.mapletalk.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mapletalk.security.JwtAuthFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

	@Value("${cors.allowed-origins}")
	private List<String> allowedOrigins;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		// Bearer-token auth, not cookies — there is no session to ride, so
		// credentialed CORS requests are neither needed nor enabled.
		configuration.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
		http
				// Stateless bearer-token API: no browser session/cookie is ever
				// issued, so there is no session-riding request for CSRF to forge.
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/health").permitAll()
						// Signup/login must be reachable without a JWT — there is no
						// token to present until one of these succeeds.
						.requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions
						// Without this, Spring Security's default entry point returns 403
						// (not 401) for an unauthenticated request to a protected endpoint.
						.authenticationEntryPoint((request, response, authException) ->
								response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
