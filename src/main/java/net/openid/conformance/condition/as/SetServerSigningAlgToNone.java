package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetServerSigningAlgToNone extends AbstractCondition {

	@Override
	@PostEnvironment(strings = "signing_algorithm")
	public Environment evaluate(Environment env) {
		env.putString("signing_algorithm", "none");
		log("Successfully set signing algorithm to none", args("signing_algorithm", "none"));
		return env;
	}

}
