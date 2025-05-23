package net.openid.conformance.vciid2issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class VCIExtractIssuerStateFromCredentialOffer extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String issuerState  = OIDFJSON.getString(env.getElementFromObject("vci", "credential_offer.grants.authorization_code.issuer_state"));
		env.putString("vci", "issuer_state", issuerState);

		logSuccess("Found issuer state", args("issuer_state", issuerState));
		return env;
	}
}
