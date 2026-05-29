package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the {@code redirect_uri} value in the verifier's response to the wallet's POST
 * to {@code response_uri}. OID4VP §8.2 makes the field OPTIONAL, but when it IS present it
 * must be a non-empty string identifying a URL the wallet should redirect the End-User to.
 * Null or empty values are not valid: omit the field instead.
 *
 * <p>Skips silently when the field is absent — that case is allowed by the spec and is
 * either acceptable (OID4VP) or handled by a separate condition
 * ({@code VP1FinalEnsureDirectPostResponseHasRedirectUriForHaip} under the HAIP profile).
 */
public class ValidateDirectPostResponseRedirectUriWhenPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = "direct_post_response")
	public Environment evaluate(Environment env) {
		JsonElement bodyEl = env.getElementFromObject("direct_post_response", "body_json");
		if (bodyEl == null || !bodyEl.isJsonObject()) {
			logSuccess("direct_post response body is not a JSON object; nothing to validate here");
			return env;
		}
		JsonObject body = bodyEl.getAsJsonObject();
		JsonElement redirectEl = body.get("redirect_uri");
		if (redirectEl == null) {
			logSuccess("redirect_uri absent from direct_post response body (OID4VP §8.2 permits this)");
			return env;
		}
		if (redirectEl.isJsonNull()) {
			throw error("redirect_uri is present with a JSON null value; OID4VP §8.2 requires a string when the field is present — omit the field instead",
				args("body_json", body));
		}
		if (!redirectEl.isJsonPrimitive() || !redirectEl.getAsJsonPrimitive().isString()) {
			throw error("redirect_uri is present but is not a string",
				args("body_json", body));
		}
		String value = OIDFJSON.getString(redirectEl);
		if (value.isEmpty()) {
			throw error("redirect_uri is present but the string value is empty; omit the field instead",
				args("body_json", body));
		}
		logSuccess("redirect_uri in direct_post response body is a non-empty string",
			args("redirect_uri", value));
		return env;
	}
}
