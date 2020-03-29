package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetServerSigningAlgToRS256 extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "signing_algorithm")
	public Environment evaluate(Environment env) {
		env.putString("signing_algorithm", "RS256");
		log("Successfully set signing algorithm to RS256");
		return env;
	}

}
