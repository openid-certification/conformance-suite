package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CheckErrorDescriptionFromAuthorizationEndpointResponseErrorContainsCRLFTAB extends AbstractCheckErrorDescriptionContainsCRLFTAB {

	@Override
	@PreEnvironment(required = "authorization_endpoint_response")
	public Environment evaluate(Environment env) {
		return checkExistCRLFTAB(env, "authorization_endpoint_response");
	}

}
