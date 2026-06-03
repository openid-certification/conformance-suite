package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Enforces HAIP 1.0 Final §5.1:
 * <blockquote>
 *   "Verifiers MUST include redirect_uri in the HTTP response to the Wallet's HTTP POST
 *   to the response_uri, as defined in Section 8.2 of [OIDF.OID4VP]."
 * </blockquote>
 * OID4VP §8.2 itself treats redirect_uri as OPTIONAL, so this stricter constraint only
 * applies under the HAIP profile and is wired separately.
 *
 * <p>Only enforces presence (absent / JSON null both fail). Whether a present value is
 * a non-empty string is checked separately by
 * {@link ValidateDirectPostResponseRedirectUriWhenPresent} so the two conditions don't
 * report the same value-shape issue twice with different requirement codes.
 */
public class VP1FinalEnsureDirectPostResponseHasRedirectUriForHaip extends AbstractCondition {

	@Override
	@PreEnvironment(required = "direct_post_response")
	public Environment evaluate(Environment env) {
		JsonElement bodyEl = env.getElementFromObject("direct_post_response", "body_json");
		if (bodyEl == null || !bodyEl.isJsonObject()) {
			throw error("direct_post response body is not a JSON object",
				args("body_json", bodyEl));
		}
		JsonObject body = bodyEl.getAsJsonObject();
		JsonElement redirectEl = body.get("redirect_uri");
		if (redirectEl == null || redirectEl.isJsonNull()) {
			throw error("HAIP 1.0 §5.1 requires the verifier to include redirect_uri in the response to the wallet's POST to response_uri, but it is missing",
				args("body_json", body));
		}
		logSuccess("Verifier included redirect_uri in the response to the wallet's POST",
			args("redirect_uri", redirectEl));
		return env;
	}
}
