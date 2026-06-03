package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractVP1FinalVpTokenDCQL extends AbstractCondition {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	@PostEnvironment(strings = {"credential"})
	public Environment evaluate(Environment env) {
		JsonElement vpToken = env.getElementFromObject("authorization_endpoint_response", "vp_token");

		if (vpToken == null) {
			throw error("Missing vp_token parameter");
		}
		if (!vpToken.isJsonObject()) {
			throw error("vp_token is not a JSON object", args("vp_token", vpToken));
		}

		JsonObject vpTokenObj = vpToken.getAsJsonObject();
		if (vpTokenObj.size() != 1) {
			throw error("vp_token seems to contain more than one credential", args("vp_token", vpToken));
		}

		String credentialId = vpTokenObj.keySet().iterator().next();
		JsonElement credentials = vpTokenObj.get(credentialId);
		String credential;
		boolean nonArray = false;
		if (credentials.isJsonArray()) {
			JsonArray credentialArray = credentials.getAsJsonArray();
			if (credentialArray.isEmpty()) {
				throw error("vp_token credential array is empty", args("vp_token", vpToken));
			}
			credential = OIDFJSON.getString(credentialArray.get(0));
		} else if (OIDFJSON.isString(credentials)) {
			credential = OIDFJSON.getString(credentials);
			nonArray = true;
		} else {
			throw error("vp_token credential value must be a string inside a JSON array", args("vp_token", vpToken));
		}

		// For the bare-string-not-in-array case we deliberately populate env before throwing so
		// that the caller (AbstractVP1FinalWalletTest, via callAndContinueOnFailure) can keep
		// validating the extracted credential against the DCQL query — wallets returning a bare
		// string instead of wrapping it in an array is a common implementation mistake worth
		// surfacing in full.
		env.putString("credential_id", credentialId);
		env.putString("credential", credential);

		if (nonArray) {
			throw error("vp_token credential must be inside a JSON array", args("vp_token", vpToken));
		}

		logSuccess("vp_token parsed", args("credentialId", credentialId, "credential", credential));

		return env;
	}

}
