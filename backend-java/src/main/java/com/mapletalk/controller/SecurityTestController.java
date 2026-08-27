package com.mapletalk.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TEMPORARY Phase 4 scaffolding only. Exists solely to prove the JWT
 * authentication filter populates the SecurityContext correctly; it is not
 * part of the Phase 5 business API and should be removed once Phase 5
 * introduces real authenticated endpoints (e.g. /api/users/me).
 */
@RestController
public class SecurityTestController {

	@GetMapping("/api/test/protected")
	public Map<String, Object> protectedResource(Authentication authentication) {
		return Map.of(
				"authenticated", true,
				"user", authentication.getName()
		);
	}

}
