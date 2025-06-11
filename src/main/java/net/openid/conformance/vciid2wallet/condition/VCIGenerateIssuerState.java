package net.openid.conformance.vciid2wallet.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

public class VCIGenerateIssuerState extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String issuerState = RandomStringUtils.secure().nextAlphabetic(42);
		env.putString("vci", "issuer_state", issuerState);

		log("Generated issuer_state", args("issuer_state", issuerState));

		return env;
	}
}
