package net.openid.conformance.security;

import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

public interface CorsConfigurable {
	List<String> getPaths();

	CorsConfiguration getCorsConfiguration();
}
