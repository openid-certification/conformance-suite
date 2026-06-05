package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AustraliaConnectIdEnsureIdTokenDoesNotContainAcr extends AbstractCondition {

	@Override
	@PreEnvironment(required = "id_token")
	public Environment evaluate(Environment env) {
		if (env.getElementFromObject("id_token", "claims.acr") != null) {
			throw error("id_token contains an acr claim, which ConnectID prohibits",
				args("acr", env.getElementFromObject("id_token", "claims.acr")));
		}

		logSuccess("id_token does not contain an acr claim");
		return env;
	}
}
