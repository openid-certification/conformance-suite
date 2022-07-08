package net.openid.conformance.security;

import com.google.common.collect.ImmutableMap;
import net.openid.conformance.runner.TestDispatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;
import java.util.Map;

@Component("additionalCorsConfiguration")
public class TestDispatcherCorsConfiguration implements CorsConfigurable {

	public static final String TEST_PATH_EXCLUDING_AUTHORIZE = TestDispatcher.TEST_PATH + "**" + "/{path:^((?!authorize).)*$}";
	public static final String TEST_MTLS_PATH_EXCLUDING_AUTHORIZE = TestDispatcher.TEST_MTLS_PATH + "**" + "/{path:^((?!authorize).)*$}";

	@Override
	public Map<String, CorsConfiguration> getCorsConfigurations() {

		CorsConfiguration testDispatcherConfiguration = new CorsConfiguration().applyPermitDefaultValues();
		testDispatcherConfiguration.setAllowedHeaders(List.of(CorsConfiguration.ALL));
		testDispatcherConfiguration.setExposedHeaders(List.of("WWW-Authenticate", "DPoP-Nonce"));

		return ImmutableMap.of(
			TEST_PATH_EXCLUDING_AUTHORIZE, testDispatcherConfiguration,
			TEST_MTLS_PATH_EXCLUDING_AUTHORIZE, testDispatcherConfiguration
		);

	}

}
