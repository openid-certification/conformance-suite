package net.openid.conformance.authzen.condition;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.ValidateUrlRequirements;
import net.openid.conformance.testmodule.Environment;

public class ValidatePDPIdentifier extends ValidateUrlRequirements {
	@Override
	@PreEnvironment(required = {"config"})
	public Environment evaluate(Environment env) {
		String label = "'Policy Decision Point Identifier' field in the test configuration";
		String fieldName = "policy_decision_point";
		JsonElement element = env.getElementFromObject("config", "pdp.policy_decision_point");
		return validateUrlRequirements(element, fieldName, label, env);
	}
}
