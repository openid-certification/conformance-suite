package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.Set;

/**
 * Sender validation: verifier_info should only contain defined properties (format, data, credential_ids).
 * See: https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-5.1-2.10.1
 */
public class CheckForUnexpectedPropertiesInVerifierInfo extends AbstractCondition {

	private static final Set<String> KNOWN_PROPERTIES = Set.of("format", "data", "credential_ids");

	@Override
	@PreEnvironment(required = { CreateEffectiveAuthorizationRequestParameters.ENV_KEY })
	public Environment evaluate(Environment env) {

		JsonObject authParameters = env.getObject(CreateEffectiveAuthorizationRequestParameters.ENV_KEY);
		if (!authParameters.has("verifier_info")) {
			log("No verifier_info found in authorization request parameters");
			return env;
		}

		JsonElement verifierInfo = authParameters.get("verifier_info");
		if (!verifierInfo.isJsonObject()) {
			// structural validation is handled by CheckVerifierInfoInVpAuthorizationRequest
			return env;
		}

		JsonObject verifierInfoObj = verifierInfo.getAsJsonObject();
		for (String key : verifierInfoObj.keySet()) {
			if (!KNOWN_PROPERTIES.contains(key)) {
				throw error("Unknown property '" + key + "' found in verifier_info in authorization request parameters",
					args("verifier_info", verifierInfo, "unknown_property", key));
			}
		}

		logSuccess("No unexpected properties found in verifier_info");

		return env;
	}
}
