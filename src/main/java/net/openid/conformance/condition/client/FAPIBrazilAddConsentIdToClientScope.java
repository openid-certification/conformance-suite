package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class FAPIBrazilAddConsentIdToClientScope extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "consent_id", required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		String consentId = env.getString("consent_id");

		JsonObject client = env.getObject("client");

		String scope = OIDFJSON.getString(client.get("scope"));
		if (Strings.isNullOrEmpty(scope)) {
			throw error("scope missing/empty in client object");
		}

		scope += " consent:"+consentId;

		client.addProperty("scope", scope);

		logSuccess("Added scope of '"+scope+"' to client's scope", client);

		return env;
	}

}
