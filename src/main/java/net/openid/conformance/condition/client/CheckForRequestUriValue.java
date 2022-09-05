package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * PAR-2.2.0: This class checks for validity of PAR response
 */
public class CheckForRequestUriValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = CallPAREndpoint.RESPONSE_KEY)
	public Environment evaluate(Environment env) {
		String requestUri = env.getString(CallPAREndpoint.RESPONSE_KEY, "body_json.request_uri");
		if (Strings.isNullOrEmpty(requestUri)) {
			throw error("request_uri is missing or empty in pushed authorization response");
		}
		try {
			@SuppressWarnings("unused")
			URI uri = new URI(requestUri);
		} catch (URISyntaxException e) {
			throw error("request_uri is malformed and does not seem to conform to RFC 2396: Uniform Resource Identifiers (URI): Generic Syntax");
		}
		logSuccess("Found valid request_uri ",  args("request_uri", requestUri));
		return env;
	}

}
