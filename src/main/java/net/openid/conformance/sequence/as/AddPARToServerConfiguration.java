package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.par.AddPushedAuthorizationRequestEndpointToServerConfig;
import net.openid.conformance.condition.as.par.AddRequirePushedAuthorizationRequestsToServerConfig;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddPARToServerConfiguration extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddPushedAuthorizationRequestEndpointToServerConfig.class, "PAR-5");
		callAndStopOnFailure(AddRequirePushedAuthorizationRequestsToServerConfig.class, "PAR-5");
	}
}
