package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class FAPIKSASetClientScopeToAccountsConsentIdOpenId extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "account_request_id", required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		String consentid = env.getString("account_request_id");

		JsonObject client = env.getObject("client");

		String scope = "accounts:" + consentid + " openid";

		client.addProperty("scope", scope);

		logSuccess("Set client's scope to '"+scope+"'", client);

		return env;
	}

}
