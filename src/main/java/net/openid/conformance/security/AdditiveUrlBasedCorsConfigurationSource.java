package net.openid.conformance.security;

import org.springframework.lang.Nullable;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Map;

public class AdditiveUrlBasedCorsConfigurationSource extends UrlBasedCorsConfigurationSource {

	@Override
	public void setCorsConfigurations(@Nullable Map<String, CorsConfiguration> corsConfigurations) {
		throw new RuntimeException("setCorsConfigurations is not allowed, use registerCorsConfiguration instead");
	}

}
