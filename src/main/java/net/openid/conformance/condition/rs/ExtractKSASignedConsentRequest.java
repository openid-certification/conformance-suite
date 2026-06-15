package net.openid.conformance.condition.rs;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;

import java.text.ParseException;

public class ExtractKSASignedConsentRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "incoming_request")
	@PostEnvironment(required = { "parsed_client_request_jwt", "new_consent_request" })
	public Environment evaluate(Environment env) {

		String body = env.getString("incoming_request", "body");
		if (Strings.isNullOrEmpty(body)) {
			throw error("The account-requests consent request body is empty; a signed JWT is required");
		}

		JsonObject parsed;
		try {
			parsed = JWTUtil.jwtStringToJsonObjectForEnvironment(body.trim());
		} catch (ParseException e) {
			throw error("Could not parse the account-requests consent request as a JWT", e, args("body", body));
		}
		if (parsed == null || !parsed.has("claims")) {
			throw error("Could not extract claims from the account-requests consent request JWT", args("body", body));
		}

		JsonObject claims = parsed.getAsJsonObject("claims");
		if (!claims.has("message") || !claims.get("message").isJsonObject()) {
			throw error("The signed consent request must contain a 'message' object", args("claims", claims));
		}

		env.putObject("parsed_client_request_jwt", parsed);
		env.putObject("new_consent_request", claims.getAsJsonObject("message"));

		logSuccess("Extracted the signed KSA consent request", args("claims", claims));
		return env;
	}
}
