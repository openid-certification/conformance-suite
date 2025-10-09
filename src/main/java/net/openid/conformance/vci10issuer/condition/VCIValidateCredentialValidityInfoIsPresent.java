package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VCIValidateCredentialValidityInfoIsPresent extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "sdjwt" } )
	public Environment evaluate(Environment env) {

		JsonObject credentialClaims = env.getElementFromObject("sdjwt", "credential.claims").getAsJsonObject();

		if (!(credentialClaims.has("exp") || credentialClaims.has("status"))) {
			throw error("Credential MUST use an exp claim, status claim or both", args("credential_claims", credentialClaims));
		}

		Map<String, Object> limitingClaims = new LinkedHashMap<>();
		for (String key : List.of("exp", "status")) {
			if (credentialClaims.has(key)) {
				limitingClaims.put(key, credentialClaims.get(key));
			}
		}

		logSuccess("Found credential validity period limited via claims " + String.join(",",limitingClaims.keySet()) + " claims", args("limiting_claims", limitingClaims));

		return env;
	}
}
