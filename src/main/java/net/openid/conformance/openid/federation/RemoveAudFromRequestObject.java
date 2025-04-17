package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveAudFromRequestObject extends AbstractCondition {

	@Override
	@PreEnvironment(required = "request_object_claims")
	public Environment evaluate(Environment env) {

		env.removeElement("request_object_claims", "aud");

		logSuccess("Removed aud from request object");

		return env;
	}
}
