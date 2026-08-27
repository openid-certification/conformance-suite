package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class AddUnknownCdrArrangementIdClaimToAuthorizationEndpointRequest extends AbstractAddCdrArrangementIdClaimToAuthorizationEndpointRequest {

	@Override
	protected String arrangementId(Environment env) {
		return "unknown-arrangement-" + UUID.randomUUID();
	}

}
