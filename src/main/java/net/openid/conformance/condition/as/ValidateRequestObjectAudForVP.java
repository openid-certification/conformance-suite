package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * Validates the request object aud claim for VP verifier tests.
 *
 * Per OID4VP 1.0 Final section 5.9, the wallet sets aud to
 * "https://self-issued.me/v2" or to the verifier's URL.
 * Both are acceptable.
 */
public class ValidateRequestObjectAudForVP extends AbstractCondition {

	private static final String SELF_ISSUED_V2 = "https://self-issued.me/v2";

	@Override
	@PreEnvironment(required = { "authorization_request_object" })
	public Environment evaluate(Environment env) {
		JsonElement aud = env.getElementFromObject("authorization_request_object", "claims.aud");
		if (aud == null) {
			throw error("Missing aud claim in request object");
		}

		// base_url is optional — if not set, only self-issued.me/v2 is accepted
		String verifierUrl = env.getString("base_url");

		if (aud.isJsonArray()) {
			boolean hasSelfIssued = aud.getAsJsonArray().contains(new JsonPrimitive(SELF_ISSUED_V2));
			boolean hasVerifierUrl = verifierUrl != null && aud.getAsJsonArray().contains(new JsonPrimitive(verifierUrl));
			if (!hasSelfIssued && !hasVerifierUrl) {
				throw error("aud claim array does not contain either 'https://self-issued.me/v2' or the verifier URL",
					args("aud", aud, "verifier_url", verifierUrl));
			}
		} else {
			String audStr = OIDFJSON.getString(aud);
			if (!SELF_ISSUED_V2.equals(audStr) && !audStr.equals(verifierUrl)) {
				throw error("aud claim does not match 'https://self-issued.me/v2' or the verifier URL",
					args("aud", aud, "verifier_url", verifierUrl));
			}
		}

		logSuccess("Request object aud claim is valid", args("aud", aud));
		return env;
	}
}
