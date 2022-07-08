package net.openid.conformance.security;

import net.openid.conformance.runner.TestDispatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Component("additionalCorsConfiguration")
public class TestDispatcherCorsConfiguration implements CorsConfigurable {

	private List<String> paths = List.of(TestDispatcher.TEST_PATH + "**", TestDispatcher.TEST_MTLS_PATH + "**");

	@Override
	public List<String> getPaths() {
		return paths;
	}

	@Override
	public CorsConfiguration getCorsConfiguration() {

		CorsConfiguration testDispatcherConfiguration = new CorsConfiguration().applyPermitDefaultValues();
		testDispatcherConfiguration.setAllowedHeaders(List.of(CorsConfiguration.ALL));
		testDispatcherConfiguration.setExposedHeaders(List.of("WWW-Authenticate,DPoP-Nonce"));

		return testDispatcherConfiguration;

	}

}
