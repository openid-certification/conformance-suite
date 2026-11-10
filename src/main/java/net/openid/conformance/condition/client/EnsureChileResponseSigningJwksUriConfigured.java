package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Checks the test configuration names where the bank publishes the key set it signs the
 * x-jws-signature of its resource responses with (CMF-XJWS-5.3.2.1). The Chile profile requires
 * clients to verify every resource response, so the field is required.
 */
public class EnsureChileResponseSigningJwksUriConfigured extends AbstractCondition {

	private static final String FIELD = "'response signing jwks uri' field in the 'Resource' section of the test configuration";

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {
		String value = env.getString("config", "resource.response_signing_jwks_uri");
		if (Strings.isNullOrEmpty(value)) {
			throw error("The " + FIELD + " is missing. The Chile profile requires the URL of the bank's response signing key set.");
		}

		URI uri;
		try {
			uri = new URI(value);
		} catch (URISyntaxException e) {
			throw error("The " + FIELD + " is not a valid URL", e, args("response_signing_jwks_uri", value));
		}
		if (!"https".equalsIgnoreCase(uri.getScheme()) || Strings.isNullOrEmpty(uri.getHost())) {
			throw error("The " + FIELD + " must be an https URL", args("response_signing_jwks_uri", value));
		}

		logSuccess("The test configuration names the bank's response signing key set", args("response_signing_jwks_uri", value));
		return env;
	}
}
