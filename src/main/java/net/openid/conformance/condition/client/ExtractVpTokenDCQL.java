package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ExtractVpTokenDCQL extends AbstractCondition {

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
		String credential = OIDFJSON.getString(vpTokenObj.get(credentialId));
		env.putString("credential_id", credentialId);
		env.putString("credential", credential);

		logSuccess("vp_token parsed", args("credentialId", credentialId, "credential", credential));

		return env;
	}

}
