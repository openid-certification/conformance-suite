package net.openid.conformance.condition.client;

import com.google.common.base.Strings;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractConsentIdFromConsentEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = "consent_endpoint_response")
	@PostEnvironment(strings = "consent_id")
	public Environment evaluate(Environment env) {
		String path = "data.consentId";

		String accountRequestId = env.getString("consent_endpoint_response", path);
		if (Strings.isNullOrEmpty(accountRequestId)) {
			throw error("Couldn't find data.consentId in the consent response");
		}

		env.putString("consent_id", accountRequestId);

		logSuccess("Extracted the consent id", args("consent_id", accountRequestId));

		return env;
	}

}