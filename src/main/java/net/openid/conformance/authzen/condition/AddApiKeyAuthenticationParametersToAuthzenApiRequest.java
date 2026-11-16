package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Sends the 'PDP API Key' in the header the 'PDP API Key Header' field names: as a Bearer token in
 * the Authorization header when the field is empty or names Authorization, which also covers a
 * pre-obtained OAuth access token, or as the whole value of any other header, such as X-API-Key
 * (openid/authzen#770).
 */
public class AddApiKeyAuthenticationParametersToAuthzenApiRequest extends AbstractCondition {

	private static final String AUTHORIZATION = "Authorization";

	/** A header field name is a token (RFC 9110 Section 5.1), made of tchar (Section 5.6.2). */
	private static final Pattern HEADER_NAME = Pattern.compile("[!#$%&'*+.^_`|~0-9A-Za-z-]+");

	/**
	 * Headers the test sends itself, or that the HTTP client manages; a key sent in one would replace
	 * it and break every request. Lower case, as header names are compared case-insensitively.
	 */
	private static final Set<String> RESERVED_HEADERS = Set.of("content-type", "accept", "accept-charset",
		"x-request-id", "host", "content-length", "transfer-encoding");

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "authzen_api_endpoint_request_headers")
	public Environment evaluate(Environment env) {
		// A JSON null is a value not entered, as for a missing field.
		JsonElement value = env.getElementFromObject("client", "api_key");
		if (value == null || value.isJsonNull()) {
			throw error("'PDP API Key' field is missing from the 'Client' section in the test configuration");
		}
		if (!OIDFJSON.isString(value)) {
			throw error("'PDP API Key' field in the 'Client' section of the test configuration is not a string",
				args("api_key", value));
		}
		String secret = OIDFJSON.getString(value);
		if (secret.isBlank()) {
			// An empty key would send an empty credential and the PDP would reject a request the
			// test meant to authenticate.
			throw error("'PDP API Key' field in the 'Client' section of the test configuration is empty");
		}

		String headerName = readHeaderName(env);

		JsonObject headers = env.getObject("authzen_api_endpoint_request_headers");

		if (headers == null) {
			headers = new JsonObject();
			env.putObject("authzen_api_endpoint_request_headers", headers);
		}

		if (AUTHORIZATION.equalsIgnoreCase(headerName)) {
			headers.addProperty(AUTHORIZATION, "Bearer " + secret);
			logSuccess("Added the PDP API key as a Bearer token in the Authorization header", headers);
		} else {
			headers.addProperty(headerName, secret);
			logSuccess("Added the PDP API key in the " + headerName + " header", headers);
		}

		return env;
	}

	private String readHeaderName(Environment env) {
		// A JSON null or an empty value is a value not entered.
		JsonElement value = env.getElementFromObject("client", "api_key_header");
		if (value == null || value.isJsonNull()) {
			return AUTHORIZATION;
		}
		if (!OIDFJSON.isString(value)) {
			throw error("'PDP API Key Header' field in the 'Client' section of the test configuration is not a string",
				args("api_key_header", value));
		}
		String name = OIDFJSON.getString(value).trim();
		if (name.isEmpty()) {
			return AUTHORIZATION;
		}
		if (!HEADER_NAME.matcher(name).matches()) {
			throw error("'PDP API Key Header' field in the 'Client' section of the test configuration is not an HTTP header name",
				args("api_key_header", name));
		}
		if (RESERVED_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
			throw error("'PDP API Key Header' field in the 'Client' section of the test configuration names a header the test "
				+ "sends itself or that the HTTP client sets (" + name + "), so the key would replace it",
				args("api_key_header", name));
		}
		return name;
	}

}
