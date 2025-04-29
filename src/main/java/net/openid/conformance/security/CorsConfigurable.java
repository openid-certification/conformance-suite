package net.openid.conformance.security;

import org.springframework.web.cors.CorsConfiguration;

import java.util.Map;

@FunctionalInterface
public interface CorsConfigurable {
	Map<String, CorsConfiguration> getCorsConfigurations();
}
