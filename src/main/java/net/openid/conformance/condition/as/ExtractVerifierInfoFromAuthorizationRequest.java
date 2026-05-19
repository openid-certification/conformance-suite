package net.openid.conformance.condition.as;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.ExtractVerifierInfoFromClientConfiguration;
import net.openid.conformance.testmodule.Environment;

public class ExtractVerifierInfoFromAuthorizationRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = CreateEffectiveAuthorizationRequestParameters.ENV_KEY)
	@PostEnvironment(required = ExtractVerifierInfoFromClientConfiguration.ENV_KEY)
	public Environment evaluate(Environment env) {
		JsonElement verifierInfo = env.getElementFromObject(
			CreateEffectiveAuthorizationRequestParameters.ENV_KEY, "verifier_info");

		if (verifierInfo == null) {
			throw error("verifier_info not found in authorization request parameters");
		}

		JsonObject wrapper = new JsonObject();
		wrapper.add(ExtractVerifierInfoFromClientConfiguration.WRAPPER_PROPERTY, verifierInfo);
		env.putObject(ExtractVerifierInfoFromClientConfiguration.ENV_KEY, wrapper);

		logSuccess("Extracted verifier_info from authorization request", args("verifier_info", verifierInfo));

		return env;
	}
}
