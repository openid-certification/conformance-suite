package net.openid.conformance.condition.as.clientattestation;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class ValidateClientAttestationIssuer extends AbstractCondition {

	@Override
	@PreEnvironment(required = "config")
	public Environment evaluate(Environment env) {

		JsonElement issEl = env.getElementFromObject("client_attestation_object", "claims.iss");

		if (issEl == null) {
			throw error("Couldn't find iss claim in the client_attestation");
		}

		String actualIssuer = OIDFJSON.getString(issEl);

		// Read the new key first; fall back to the legacy vci.* key so existing stored
		// test configs keep working through a transition window.
		String expectedIssuer = env.getString("config", "client_attestation.issuer");
		if (expectedIssuer == null) {
			expectedIssuer = env.getString("config", "vci.client_attestation_issuer");
		}
		if (!expectedIssuer.equals(actualIssuer)) {
			throw error("Found issuer mismatch in client attestation", args("expected_issuer", expectedIssuer, "actual_issuer", actualIssuer));
		}

		logSuccess("Found matching issuer in client attestation", args("expected_issuer", expectedIssuer, "actual_issuer", actualIssuer));

		return env;
	}
}
