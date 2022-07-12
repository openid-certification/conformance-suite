package net.openid.conformance.security;

import org.springframework.web.cors.CorsConfiguration;

import java.util.Map;

public interface CorsConfigurable {
	Map<String, CorsConfiguration> getCorsConfigurations();
}
