package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * See: https://openid.net/specs/openid-4-verifiable-presentations-1_0.html#section-5.1-2.10.1
 */
public class CheckVerifierInfoInVpAuthorizationRequest extends AbstractCondition {

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
			throw error("verifier_info in authorization request parameters is not a JSON object", args("verifier_info", verifierInfo));
		}

		JsonObject verifierInfoObj = verifierInfo.getAsJsonObject();
		if (!verifierInfoObj.has("format")) {
			throw error("Required 'format' attribute is missing in verifier_info", args("verifier_info", verifierInfo));
		}

		JsonElement format = verifierInfoObj.get("format");
		if (!(format.isJsonPrimitive() && format.getAsJsonPrimitive().isString())) {
			throw error("'format' attribute in verifier_info in authorization request parameters must be a string", args("format", format));
		}

		if (!verifierInfoObj.has("data")) {
			throw error("Required 'data' attribute is missing in verifier_info", args("verifier_info", verifierInfo));
		}

		JsonElement data = verifierInfoObj.get("data");
		if (!((data.isJsonPrimitive() && data.getAsJsonPrimitive().isString()) || data.isJsonObject())) {
			throw error("'data' attribute in verifier_info in authorization request parameters must be a string or JSON object", args("data", data));
		}

		if (verifierInfoObj.has("credential_ids")) {
			JsonElement credentialIds = verifierInfoObj.get("credential_ids");
			if (!credentialIds.isJsonArray()) {
				throw error("Optional 'credential_ids' attribute in verifier_info in authorization request parameters must be an array if present", args("credential_ids", credentialIds));
			}

			JsonArray credentialIdsArray = credentialIds.getAsJsonArray();
			if (credentialIdsArray.isEmpty()) {
				throw error("Optional 'credential_ids' attribute in verifier_info in authorization request parameters must be an empty array if present", args("credential_ids", credentialIds));
			}

			// for more advanced validation we could also cross-check the credential_ids with the credentials from the dcql_query
		}

		logSuccess("Found valid verifier_info in authorization request parameters", args("verifier_info", verifierInfo));

		return env;
	}
}
