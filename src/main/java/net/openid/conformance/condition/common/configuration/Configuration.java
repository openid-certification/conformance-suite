package net.openid.conformance.condition.common.configuration;

import net.openid.conformance.condition.FunctionalCondition;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class Configuration {
	public static FunctionalCondition.Body checkPlainUrlIsConfigured(String propertyPath, String propertyName, String section) {
		return (env, c) -> {
			String propertyValue = env.getString("config", propertyPath);
			if (propertyValue == null || propertyValue.isBlank()) {
				throw c.error("'" + propertyName + "' field is missing from the '" + section + "' section in the test configuration");
			}
			URI uri;
			try {
				uri = new URI(propertyValue);
			} catch (URISyntaxException e) {
				throw c.error("'" + propertyName + "' in the '" + section + "' section of the test configuration is not a valid URL",
					e, Map.of(propertyName, propertyValue));
			}
			if (!"https".equals(uri.getScheme()) || uri.getHost() == null
				|| uri.getRawQuery() != null || uri.getRawFragment() != null) {
				throw c.error("'" + propertyName + "' in the '" + section + "' section of the test configuration must be an https URL "
					+ "without query or fragment", Map.of(propertyName, propertyValue));
			}
			c.logSuccess("'" + propertyName + "' is configured", Map.of(propertyName, propertyValue));
		};
	}

}
