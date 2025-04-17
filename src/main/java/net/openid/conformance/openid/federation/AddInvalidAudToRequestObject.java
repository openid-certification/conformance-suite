package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddInvalidAudToRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		String aud = env.getString("request_object_claims", "aud");
		String invalidAud = aud + "/1";
		env.putString("request_object_claims", "aud", invalidAud);

		logSuccess("Added invalid aud to request object", args("aud", invalidAud));

		return env;
	}
}
