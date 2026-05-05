package net.openid.conformance.condition.as;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class RemoveScopeFromTokenEndpointResponse extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"token_endpoint_response"})
	public Environment evaluate(Environment env) {

		env.removeElement("token_endpoint_response", "scope");

		logSuccess("Removed scope from token endpoint response");

		return env;

	}

}
