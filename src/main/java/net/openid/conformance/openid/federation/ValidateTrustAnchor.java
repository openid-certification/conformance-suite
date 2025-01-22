package net.openid.conformance.openid.federation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ValidateTrustAnchor extends ValidateUrlRequirements {

	@Override
	@PreEnvironment(required = { "config" } )
	public Environment evaluate(Environment env) {
		String label = "Trust anchor";
		String fieldName = "trust_anchor";
		JsonElement element = env.getElementFromObject("config", "federation.trust_anchor");
		return validateUrlRequirements(element, fieldName, label, env);
	}

}
