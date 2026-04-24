package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

/**
 * Extracts the JWT body from the status list token endpoint response and stores
 * it in the standard environment format with value, header, and claims fields.
 *
 * Skips when no status list fetch was performed (credential had no status
 * claim).
 */
public class ExtractStatusListTokenFromStatusListTokenEndpointResponse extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		env.removeObject("status_list_token");

		if (!env.containsObject("status_list_token_endpoint_response")) {
			log("No status list token endpoint response recorded, skipping JWT extraction");
			return env;
		}

		JsonElement responseBody = env.getElementFromObject("status_list_token_endpoint_response", "body");
		if (responseBody == null ||
			!responseBody.isJsonPrimitive() ||
			!responseBody.getAsJsonPrimitive().isString()) {
			throw error("The status list token endpoint response body is expected to contain a JWT string",
				args("response_body", responseBody));
		}

		String jwt = OIDFJSON.getString(responseBody);
		try {
			JsonObject statusListToken = JWTUtil.jwtStringToJsonObjectForEnvironment(jwt.trim());
			env.putObject("status_list_token", statusListToken);
			logSuccess("Extracted status list token from status list token endpoint response",
				args("status_list_token", statusListToken));
			return env;
		} catch (ParseException | OIDFJSON.UnexpectedJsonTypeException e) {
			env.removeObject("status_list_token");
			throw error("Failed to parse status list token JWT from status list token endpoint response", e,
				args("jwt", jwt));
		}
	}
}
