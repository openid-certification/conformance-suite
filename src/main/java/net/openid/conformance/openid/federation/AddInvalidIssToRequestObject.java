package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidIssToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		String iss = env.getString("request_object_claims", "iss");
		String invalidIss = iss + "/1";
		env.putString("request_object_claims", "iss", invalidIss);

		logSuccess("Added invalid iss to request object", args("iss", invalidIss));

		return env;
	}
}
