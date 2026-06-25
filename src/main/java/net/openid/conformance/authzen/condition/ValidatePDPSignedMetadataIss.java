package net.openid.conformance.authzen.condition;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;


public class ValidatePDPSignedMetadataIss extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"pdp_signed_metadata", "config"})
	public Environment evaluate(Environment env) {
		String actualIssuer = env.getString("pdp_signed_metadata", "claims.iss");
		if(Strings.isNullOrEmpty(actualIssuer)) {
			throw error("PDP `signed_metadata` MUST contain an `iss` (issuer) claim", args("issuer", actualIssuer));
		}

		String expectedIssuer = env.getString("config", "pdp.policy_decision_point");
		if (Strings.isNullOrEmpty(expectedIssuer)) {
			throw error("'Policy Decision Point Identifier' field is missing from the 'PDP' section in the test configuration");
		}

		if (!expectedIssuer.equals(actualIssuer)) {
			throw error("PDP issuer mismatch in signed_metadata", args("expected", expectedIssuer, "actual", actualIssuer));
		}

		logSuccess("Found matching issuer in PDP `signed_metadata`", args("expected", expectedIssuer, "actual", actualIssuer));
		return env;
	}

}
